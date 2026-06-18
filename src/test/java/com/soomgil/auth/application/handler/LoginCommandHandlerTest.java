package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.LoginCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.application.service.PasswordHasher;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.PasswordCredential;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordCredentialMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginCommandHandlerTest {

	private final UserMapper userMapper = mock(UserMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final PasswordCredentialMapper passwordCredentialMapper = mock(PasswordCredentialMapper.class);
	private final UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);
	private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
	private final AuthTokenService authTokenService = mock(AuthTokenService.class);

	private final LoginCommandHandler handler = new LoginCommandHandler(
		userMapper,
		emailAddressMapper,
		passwordCredentialMapper,
		userProfileMapper,
		userSessionMapper,
		passwordHasher,
		authTokenService
	);

	private AuthUser activeUser(UUID userId) {
		return new AuthUser(userId, UserStatus.ACTIVE, null, Instant.now().minusSeconds(60));
	}

	@Test
	@DisplayName("정상 로그인 시 access/refresh token을 발급하고 실패 count를 reset한다")
	void logsInSuccessfully() {
		UUID userId = UUID.randomUUID();
		UUID emailId = UUID.randomUUID();
		EmailAddress email = new EmailAddress(emailId, userId, "user@example.com", "user@example.com", true, null);
		AuthUser user = activeUser(userId);
		PasswordCredential credential = new PasswordCredential(userId, "stored-hash", 0, null);

		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com")).thenReturn(Optional.of(email));
		when(userMapper.findById(userId)).thenReturn(Optional.of(user));
		when(passwordCredentialMapper.findByUserId(userId)).thenReturn(Optional.of(credential));
		when(passwordHasher.matches("P@ssw0rd!", "stored-hash")).thenReturn(true);
		when(userProfileMapper.findDisplayName(userId)).thenReturn(Optional.of("민지"));
		when(authTokenService.mintAccessToken(userId, "user@example.com")).thenReturn("access-token");
		when(authTokenService.mintRefreshToken()).thenReturn(
			new AuthTokenService.IssuedRefreshToken("raw-refresh", "hash-refresh", Instant.now().plusSeconds(3600))
		);

		AuthTokenResult result = handler.handle(new LoginCommand("User@Example.com", "P@ssw0rd!"));

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("raw-refresh");
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.email()).isEqualTo("user@example.com");
		assertThat(result.displayName()).isEqualTo("민지");

		verify(passwordCredentialMapper).resetFailedLoginCount(userId);
		verify(userMapper).updateLastLoginAt(eq(userId), any(Instant.class));
		verify(userSessionMapper).insert(any(UUID.class), eq(userId), eq("hash-refresh"), any(UUID.class), any(Instant.class));
	}

	@Test
	@DisplayName("이메일로 계정을 찾을 수 없으면 INVALID_CREDENTIALS 예외를 던진다")
	void throwsWhenEmailNotFound() {
		when(emailAddressMapper.findActiveByNormalizedEmail(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new LoginCommand("none@example.com", "pw")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		verify(passwordCredentialMapper, never()).resetFailedLoginCount(any(UUID.class));
		verify(userSessionMapper, never()).insert(any(UUID.class), any(UUID.class), anyString(), any(UUID.class), any(Instant.class));
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외를 던진다")
	void throwsWhenPasswordMismatch() {
		UUID userId = UUID.randomUUID();
		EmailAddress email = new EmailAddress(UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, null);
		AuthUser user = activeUser(userId);
		PasswordCredential credential = new PasswordCredential(userId, "stored-hash", 0, null);

		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com")).thenReturn(Optional.of(email));
		when(userMapper.findById(userId)).thenReturn(Optional.of(user));
		when(passwordCredentialMapper.findByUserId(userId)).thenReturn(Optional.of(credential));
		when(passwordHasher.matches("wrong", "stored-hash")).thenReturn(false);

		assertThatThrownBy(() -> handler.handle(new LoginCommand("user@example.com", "wrong")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		verify(passwordCredentialMapper, never()).resetFailedLoginCount(userId);
		verify(authTokenService, never()).mintAccessToken(any(UUID.class), anyString());
	}

	@Test
	@DisplayName("비활성 계정은 USER_INACTIVE 예외를 던진다")
	void throwsWhenUserInactive() {
		UUID userId = UUID.randomUUID();
		EmailAddress email = new EmailAddress(UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, null);
		AuthUser suspended = new AuthUser(userId, UserStatus.SUSPENDED, null, Instant.now());

		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com")).thenReturn(Optional.of(email));
		when(userMapper.findById(userId)).thenReturn(Optional.of(suspended));

		assertThatThrownBy(() -> handler.handle(new LoginCommand("user@example.com", "pw")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.USER_INACTIVE);
	}
}
