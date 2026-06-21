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
import com.soomgil.auth.application.command.RefreshCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.UserSession;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshCommandHandlerTest {

	private final UserMapper userMapper = mock(UserMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);
	private final AuthTokenService authTokenService = mock(AuthTokenService.class);

	private final RefreshCommandHandler handler = new RefreshCommandHandler(
		userMapper, emailAddressMapper, userProfileMapper, userSessionMapper, authTokenService
	);

	private UserSession activeSession(UUID sessionId, UUID userId) {
		return new UserSession(
			sessionId, userId, "token-hash", UUID.randomUUID(),
			Instant.now().plusSeconds(3600), null, Instant.now().minusSeconds(60)
		);
	}

	@Test
	@DisplayName("정상 rotation 시 기존 session을 revoke하고 같은 family로 새 refresh token을 발급한다")
	void rotatesRefreshTokenSuccessfully() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID familyId = UUID.randomUUID();
		UserSession session = new UserSession(
			sessionId, userId, "hash", familyId,
			Instant.now().plusSeconds(3600), null, Instant.now()
		);
		AuthUser user = new AuthUser(userId, UserStatus.ACTIVE, null, Instant.now());

		when(authTokenService.hashRefreshToken("raw-token")).thenReturn("hash");
		when(userSessionMapper.findByRefreshTokenHash("hash")).thenReturn(Optional.of(session));
		when(userMapper.findById(userId)).thenReturn(Optional.of(user));
		when(emailAddressMapper.findPrimaryByUserId(userId)).thenReturn(
			Optional.of(new EmailAddress(UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, null))
		);
		when(userProfileMapper.findDisplayName(userId)).thenReturn(Optional.of("민지"));
		when(authTokenService.mintAccessToken(userId, "user@example.com")).thenReturn("new-access");
		when(authTokenService.mintRefreshToken()).thenReturn(
			new AuthTokenService.IssuedRefreshToken("new-raw", "new-hash", Instant.now().plusSeconds(7200))
		);

		AuthTokenResult result = handler.handle(new RefreshCommand("raw-token"));

		assertThat(result.accessToken()).isEqualTo("new-access");
		assertThat(result.refreshToken()).isEqualTo("new-raw");
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.email()).isEqualTo("user@example.com");
		assertThat(result.displayName()).isEqualTo("민지");

		verify(userSessionMapper).revoke(eq(sessionId), any(Instant.class), eq("ROTATED"));
		verify(userSessionMapper).insert(any(UUID.class), eq(userId), eq("new-hash"), eq(familyId), any(Instant.class));
	}

	@Test
	@DisplayName("이미 revoke된 session의 token이 재사용되면 family 전체를 revoke하고 REFRESH_TOKEN_REUSE_DETECTED 예외를 던진다")
	void detectsTokenReuseAndRevokesFamily() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID familyId = UUID.randomUUID();
		UserSession revokedSession = new UserSession(
			sessionId, userId, "hash", familyId,
			Instant.now().plusSeconds(3600), Instant.now().minusSeconds(60), Instant.now()
		);

		when(authTokenService.hashRefreshToken("raw-token")).thenReturn("hash");
		when(userSessionMapper.findByRefreshTokenHash("hash")).thenReturn(Optional.of(revokedSession));

		assertThatThrownBy(() -> handler.handle(new RefreshCommand("raw-token")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

		verify(userSessionMapper).revokeFamily(eq(familyId), any(Instant.class), eq("REUSE_DETECTED"));
		verify(authTokenService, never()).mintAccessToken(any(UUID.class), anyString());
	}

	@Test
	@DisplayName("session을 찾을 수 없으면 REFRESH_TOKEN_INVALID 예외를 던진다")
	void throwsWhenSessionNotFound() {
		when(authTokenService.hashRefreshToken(anyString())).thenReturn("hash");
		when(userSessionMapper.findByRefreshTokenHash("hash")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new RefreshCommand("raw-token")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
	}

	@Test
	@DisplayName("만료된 session은 REFRESH_TOKEN_INVALID 예외를 던진다")
	void throwsWhenSessionExpired() {
		UUID userId = UUID.randomUUID();
		UserSession expired = new UserSession(
			UUID.randomUUID(), userId, "hash", UUID.randomUUID(),
			Instant.now().minusSeconds(60), null, Instant.now().minusSeconds(120)
		);

		when(authTokenService.hashRefreshToken("raw-token")).thenReturn("hash");
		when(userSessionMapper.findByRefreshTokenHash("hash")).thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> handler.handle(new RefreshCommand("raw-token")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
	}
}
