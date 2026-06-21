package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.application.query.GetMySettingsQuery;
import com.soomgil.user.domain.model.UserSettingsRecord;
import com.soomgil.user.infrastructure.persistence.UserMeSettingsMapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link GetMySettingsQueryHandler} 단위 테스트.
 *
 * <p>{@link UserMeSettingsMapper}를 Mockito 목으로 대체하여,
 * 정상 조회 흐름과 row 부재 시 기본값 대체 흐름을 검증한다.
 */
class GetMySettingsQueryHandlerTest {

	private final UserMeSettingsMapper mapper = mock(UserMeSettingsMapper.class);
	private final GetMySettingsQueryHandler handler = new GetMySettingsQueryHandler(mapper);

	@Test
	@DisplayName("설정 row가 있으면 DB 값을 그대로 응답한다")
	void returnsRowFromDb() {
		UUID userId = UUID.randomUUID();
		OffsetDateTime optedInAt = OffsetDateTime.now().minusDays(1);
		UserSettingsRecord record = new UserSettingsRecord(
			userId, "en", "Europe/London", true, optedInAt, null, false
		);
		when(mapper.findByUserId(userId)).thenReturn(Optional.of(record));

		UserSettings settings = handler.handle(new GetMySettingsQuery(userId));

		assertThat(settings.displayLanguage()).isEqualTo("en");
		assertThat(settings.timezone()).isEqualTo("Europe/London");
		assertThat(settings.marketingEmailOptIn()).isTrue();
		assertThat(settings.marketingEmailOptedInAt()).isEqualTo(optedInAt);
		assertThat(settings.marketingEmailOptedOutAt()).isNull();
		assertThat(settings.tripInviteEmailOptIn()).isFalse();
	}

	@Test
	@DisplayName("설정 row가 없으면 기본값(ko, Asia/Seoul, marketing false, tripInvite true)으로 대체한다")
	void fallsBackToDefaultsWhenRowMissing() {
		UUID userId = UUID.randomUUID();
		when(mapper.findByUserId(userId)).thenReturn(Optional.empty());

		UserSettings settings = handler.handle(new GetMySettingsQuery(userId));

		assertThat(settings.displayLanguage()).isEqualTo("ko");
		assertThat(settings.timezone()).isEqualTo("Asia/Seoul");
		assertThat(settings.marketingEmailOptIn()).isFalse();
		assertThat(settings.tripInviteEmailOptIn()).isTrue();
	}
}
