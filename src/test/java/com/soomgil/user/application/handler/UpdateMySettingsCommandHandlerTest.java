package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.application.command.UpdateMySettingsCommand;
import com.soomgil.user.domain.model.UserException;
import com.soomgil.user.domain.model.UserSettingsRecord;
import com.soomgil.user.infrastructure.persistence.UserMeSettingsMapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link UpdateMySettingsCommandHandler} 단위 테스트.
 *
 * <p>부분 update, validation, marketing timestamp 전환 동기화, row 부재 실패를 검증한다.
 */
class UpdateMySettingsCommandHandlerTest {

	private final UserMeSettingsMapper settingsMapper = mock(UserMeSettingsMapper.class);
	private final UpdateMySettingsCommandHandler handler =
		new UpdateMySettingsCommandHandler(settingsMapper);

	@Test
	@DisplayName("command의 null이 아닌 필드만 merge하여 write mapper로 전체 row를 쓴다")
	void mergesNonNullFieldsAndWritesFullRow() {
		UUID userId = UUID.randomUUID();
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", false, null, null, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));
		when(settingsMapper.updateRecord(eq(userId), any(UserSettingsRecord.class))).thenReturn(1);

		UserSettings result = handler.handle(new UpdateMySettingsCommand(
			userId, "en", null, null, null
		));

		assertThat(result.displayLanguage()).isEqualTo("en");
		assertThat(result.timezone()).isEqualTo("Asia/Seoul");
		assertThat(result.marketingEmailOptIn()).isFalse();
		assertThat(result.tripInviteEmailOptIn()).isTrue();

		verify(settingsMapper, times(1)).updateRecord(eq(userId), any(UserSettingsRecord.class));
	}

	@Test
	@DisplayName("marketingEmailOptIn이 false에서 true로 전환되면 opted_in_at을 기록한다")
	void recordsMarketingOptInTimestampOnOptIn() {
		UUID userId = UUID.randomUUID();
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", false, null, null, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));
		when(settingsMapper.updateRecord(eq(userId), any(UserSettingsRecord.class))).thenReturn(1);

		OffsetDateTime before = OffsetDateTime.now();
		UserSettings result = handler.handle(new UpdateMySettingsCommand(
			userId, null, null, true, null
		));
		OffsetDateTime after = OffsetDateTime.now();

		assertThat(result.marketingEmailOptIn()).isTrue();
		assertThat(result.marketingEmailOptedInAt()).isBetween(before, after);
		assertThat(result.marketingEmailOptedOutAt()).isNull();
	}

	@Test
	@DisplayName("marketingEmailOptIn이 true에서 false로 전환되면 opted_out_at을 기록한다")
	void recordsMarketingOptOutTimestampOnOptOut() {
		UUID userId = UUID.randomUUID();
		OffsetDateTime optedInAt = OffsetDateTime.now().minusDays(1);
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", true, optedInAt, null, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));
		when(settingsMapper.updateRecord(eq(userId), any(UserSettingsRecord.class))).thenReturn(1);

		OffsetDateTime before = OffsetDateTime.now();
		UserSettings result = handler.handle(new UpdateMySettingsCommand(
			userId, null, null, false, null
		));
		OffsetDateTime after = OffsetDateTime.now();

		assertThat(result.marketingEmailOptIn()).isFalse();
		assertThat(result.marketingEmailOptedInAt()).isEqualTo(optedInAt);
		assertThat(result.marketingEmailOptedOutAt()).isBetween(before, after);
	}

	@Test
	@DisplayName("marketingEmailOptIn 값이 동일하면 timestamp를 변경하지 않는다")
	void doesNotTouchTimestampsWhenOptInUnchanged() {
		UUID userId = UUID.randomUUID();
		OffsetDateTime optedInAt = OffsetDateTime.now().minusDays(7);
		OffsetDateTime optedOutAt = OffsetDateTime.now().minusDays(3);
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", false, optedInAt, optedOutAt, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));
		when(settingsMapper.updateRecord(eq(userId), any(UserSettingsRecord.class))).thenReturn(1);

		UserSettings result = handler.handle(new UpdateMySettingsCommand(
			userId, null, null, false, null
		));

		assertThat(result.marketingEmailOptedInAt()).isEqualTo(optedInAt);
		assertThat(result.marketingEmailOptedOutAt()).isEqualTo(optedOutAt);
	}

	@Test
	@DisplayName("marketingEmailOptIn이 명시되지 않으면 timestamp를 변경하지 않는다")
	void doesNotTouchTimestampsWhenOptInNotProvided() {
		UUID userId = UUID.randomUUID();
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", false, null, null, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));
		when(settingsMapper.updateRecord(eq(userId), any(UserSettingsRecord.class))).thenReturn(1);

		UserSettings result = handler.handle(new UpdateMySettingsCommand(
			userId, "en", null, null, false
		));

		assertThat(result.marketingEmailOptedInAt()).isNull();
		assertThat(result.marketingEmailOptedOutAt()).isNull();
		assertThat(result.tripInviteEmailOptIn()).isFalse();
	}

	@Test
	@DisplayName("유효하지 않은 timezone은 INVALID_TIMEZONE 예외를 던지고 write mapper를 호출하지 않는다")
	void rejectsInvalidTimezone() {
		UUID userId = UUID.randomUUID();
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", false, null, null, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));

		assertThatThrownBy(() -> handler.handle(new UpdateMySettingsCommand(
			userId, null, "Foo/Bar", null, null
		)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_TIMEZONE);

		verify(settingsMapper, never()).updateRecord(any(UUID.class), any(UserSettingsRecord.class));
	}

	@Test
	@DisplayName("지원하지 않는 표시 언어는 INVALID_DISPLAY_LANGUAGE 예외를 던진다")
	void rejectsUnsupportedLanguage() {
		UUID userId = UUID.randomUUID();
		UserSettingsRecord current = new UserSettingsRecord(
			userId, "ko", "Asia/Seoul", false, null, null, true
		);
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.of(current));

		assertThatThrownBy(() -> handler.handle(new UpdateMySettingsCommand(
			userId, "ja", null, null, null
		)))
			.isInstanceOf(UserException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_DISPLAY_LANGUAGE);
	}

	@Test
	@DisplayName("설정 row가 없으면 PROFILE_NOT_FOUND 예외를 던진다")
	void throwsWhenRowMissing() {
		UUID userId = UUID.randomUUID();
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new UpdateMySettingsCommand(
			userId, "en", null, null, null
		)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
	}
}
