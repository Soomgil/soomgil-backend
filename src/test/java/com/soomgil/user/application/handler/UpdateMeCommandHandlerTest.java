package com.soomgil.user.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.application.command.UpdateMeCommand;
import com.soomgil.user.domain.model.UserProfileRecord;
import com.soomgil.user.infrastructure.persistence.UserMeMapper;
import com.soomgil.user.infrastructure.persistence.UserMeSettingsMapper;
import com.soomgil.media.infrastructure.persistence.MediaFileMapper;
import com.soomgil.media.infrastructure.persistence.MediaFileRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link UpdateMeCommandHandler} 단위 테스트.
 */
class UpdateMeCommandHandlerTest {

	private final UserMeMapper userMeMapper = mock(UserMeMapper.class);
	private final UserMeSettingsMapper settingsMapper = mock(UserMeSettingsMapper.class);
	private final UserMapper authUserMapper = mock(UserMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final MediaFileMapper mediaFileMapper = mock(MediaFileMapper.class);
	private final UpdateMeCommandHandler handler = new UpdateMeCommandHandler(
		userMeMapper, settingsMapper, authUserMapper, emailAddressMapper, mediaFileMapper
	);

	@Test
	@DisplayName("command의 null이 아닌 필드만 merge하여 update하고 갱신된 User를 반환한다")
	void mergesNonNullFieldsAndReturnsUpdatedUser() {
		UUID userId = UUID.randomUUID();
		UUID mediaFileId = UUID.randomUUID();
		UserProfileRecord current = new UserProfileRecord(
			userId, "oldName", null, null, "old bio", UserProfileVisibility.PUBLIC
		);
		AuthUser authUser = new AuthUser(userId, UserStatus.ACTIVE, null, Instant.now());
		EmailAddress email = new EmailAddress(UUID.randomUUID(), userId, "minji@example.com",
			"minji@example.com", true, Instant.now());

		when(userMeMapper.findFull(userId)).thenReturn(Optional.of(current));
		MediaFileRecord mediaFile = new MediaFileRecord(
			mediaFileId, userId, "LOCAL", "default-bucket", "filename.jpg", "http://localhost:8080/uploads/filename.jpg",
			"image/jpeg", 1024L, null, null, null, null, "ACTIVE", Instant.now()
		);
		when(mediaFileMapper.findById(mediaFileId)).thenReturn(Optional.of(mediaFile));
		when(userMeMapper.updateRecord(any(UserProfileRecord.class))).thenReturn(1);
		when(authUserMapper.findById(userId)).thenReturn(Optional.of(authUser));
		when(emailAddressMapper.findPrimaryByUserId(userId)).thenReturn(Optional.of(email));
		when(settingsMapper.findByUserId(userId)).thenReturn(Optional.empty());

		User result = handler.handle(new UpdateMeCommand(
			userId, "민지", mediaFileId, null, UserProfileVisibility.PRIVATE
		));

		assertThat(result.profile().displayName()).isEqualTo("민지");
		assertThat(result.profile().profileMediaFileId()).isEqualTo(mediaFileId);
		assertThat(result.profile().bio()).isEqualTo("old bio");
		assertThat(result.profile().profileVisibility()).isEqualTo(UserProfileVisibility.PRIVATE);
		assertThat(result.primaryEmail()).isEqualTo("minji@example.com");

		verify(userMeMapper, times(1)).updateRecord(any(UserProfileRecord.class));
	}

	@Test
	@DisplayName("profile row가 없으면 PROFILE_NOT_FOUND 예외를 던진다")
	void throwsWhenProfileRowMissing() {
		UUID userId = UUID.randomUUID();
		when(userMeMapper.findFull(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new UpdateMeCommand(
			userId, "민지", null, null, null
		)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
	}

	@Test
	@DisplayName("빈 display name은 BUSINESS_RULE_VIOLATION 예외를 던진다")
	void rejectsEmptyDisplayName() {
		UUID userId = UUID.randomUUID();
		UserProfileRecord current = new UserProfileRecord(
			userId, "oldName", null, null, null, UserProfileVisibility.PUBLIC
		);
		when(userMeMapper.findFull(userId)).thenReturn(Optional.of(current));

		assertThatThrownBy(() -> handler.handle(new UpdateMeCommand(
			userId, "", null, null, null
		)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	@DisplayName("501자 bio는 BUSINESS_RULE_VIOLATION 예외를 던진다")
	void rejectsBioOver500() {
		UUID userId = UUID.randomUUID();
		UserProfileRecord current = new UserProfileRecord(
			userId, "oldName", null, null, null, UserProfileVisibility.PUBLIC
		);
		when(userMeMapper.findFull(userId)).thenReturn(Optional.of(current));

		assertThatThrownBy(() -> handler.handle(new UpdateMeCommand(
			userId, null, null, "a".repeat(501), null
		)))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException) e).errorCode())
			.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}
}
