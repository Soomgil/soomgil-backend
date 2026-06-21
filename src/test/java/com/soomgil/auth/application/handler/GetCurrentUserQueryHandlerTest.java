package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.query.GetCurrentUserQuery;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.api.dto.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetCurrentUserQueryHandlerTest {

	private final UserMapper userMapper = mock(UserMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);

	private final GetCurrentUserQueryHandler handler = new GetCurrentUserQueryHandler(
		userMapper, emailAddressMapper, userProfileMapper
	);

	@Test
	@DisplayName("사용자 ID로 프로필을 조회하여 User DTO를 구성한다")
	void returnsCurrentUserProfile() {
		UUID userId = UUID.randomUUID();
		AuthUser user = new AuthUser(userId, UserStatus.ACTIVE, Instant.now(), Instant.now().minusSeconds(3600));
		EmailAddress email = new EmailAddress(
			UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, Instant.now()
		);

		when(userMapper.findById(userId)).thenReturn(Optional.of(user));
		when(emailAddressMapper.findPrimaryByUserId(userId)).thenReturn(Optional.of(email));
		when(userProfileMapper.findFull(userId)).thenReturn(Optional.of(
			new com.soomgil.user.domain.model.UserProfileRecord(
				userId, "민지", null, null, null, com.soomgil.user.api.dto.UserProfileVisibility.PUBLIC
			)
		));

		User result = handler.handle(new GetCurrentUserQuery(userId));

		assertThat(result.id()).isEqualTo(userId);
		assertThat(result.primaryEmail()).isEqualTo("user@example.com");
		assertThat(result.primaryEmailVerifiedAt()).isNotNull();
		assertThat(result.status()).isEqualTo(com.soomgil.user.api.dto.UserStatus.ACTIVE);
		assertThat(result.profile().displayName()).isEqualTo("민지");
	}

	@Test
	@DisplayName("사용자를 찾을 수 없으면 USER_NOT_FOUND 예외를 던진다")
	void throwsWhenUserNotFound() {
		UUID userId = UUID.randomUUID();
		when(userMapper.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new GetCurrentUserQuery(userId)))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("이메일이나 프로필이 없어도 기본값으로 응답을 구성한다")
	void handlesMissingEmailAndProfileGracefully() {
		UUID userId = UUID.randomUUID();
		AuthUser user = new AuthUser(userId, UserStatus.ACTIVE, null, Instant.now());

		when(userMapper.findById(userId)).thenReturn(Optional.of(user));
		when(emailAddressMapper.findPrimaryByUserId(userId)).thenReturn(Optional.empty());
		when(userProfileMapper.findFull(userId)).thenReturn(Optional.empty());

		User result = handler.handle(new GetCurrentUserQuery(userId));

		assertThat(result.primaryEmail()).isNull();
		assertThat(result.primaryEmailVerifiedAt()).isNull();
		assertThat(result.profile().displayName()).isEmpty();
	}
}
