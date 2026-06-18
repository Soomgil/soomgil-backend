package com.soomgil.auth.application.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.command.LogoutCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.domain.model.UserSession;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.cqrs.NoResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogoutCommandHandlerTest {

	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);
	private final AuthTokenService authTokenService = mock(AuthTokenService.class);

	private final LogoutCommandHandler handler = new LogoutCommandHandler(userSessionMapper, authTokenService);

	@Test
	@DisplayName("allDevices=true면 revokeAllForUser를 호출한다")
	void revokesAllSessionsWhenAllDevices() {
		UUID userId = UUID.randomUUID();

		NoResult result = handler.handle(new LogoutCommand(userId, "any-token", true));

		verify(userSessionMapper).revokeAllForUser(eq(userId), any(Instant.class), eq("USER_LOGOUT_ALL"));
		verify(authTokenService, never()).hashRefreshToken(anyString());
	}

	@Test
	@DisplayName("refreshToken이 있으면 hash로 session을 찾아 revoke한다")
	void revokesSingleSessionByRefreshToken() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		UserSession session = new UserSession(
			sessionId, userId, "hash", UUID.randomUUID(),
			Instant.now().plusSeconds(60), null, Instant.now()
		);

		when(authTokenService.hashRefreshToken("raw-token")).thenReturn("hash");
		when(userSessionMapper.findByRefreshTokenHash("hash")).thenReturn(Optional.of(session));

		handler.handle(new LogoutCommand(userId, "raw-token", false));

		verify(userSessionMapper).revoke(eq(sessionId), any(Instant.class), eq("USER_LOGOUT"));
	}

	@Test
	@DisplayName("refreshToken이 null이면 아무 것도 revoke하지 않는다")
	void doesNothingWhenTokenBlank() {
		UUID userId = UUID.randomUUID();

		handler.handle(new LogoutCommand(userId, null, false));

		verify(userSessionMapper, never()).revoke(any(UUID.class), any(Instant.class), anyString());
		verify(userSessionMapper, never()).revokeAllForUser(any(UUID.class), any(Instant.class), anyString());
	}

	@Test
	@DisplayName("refreshToken hash로 session을 찾지 못해도 예외 없이 NoResult를 반환한다")
	void returnsNoResultWhenSessionNotFoundByToken() {
		UUID userId = UUID.randomUUID();
		when(authTokenService.hashRefreshToken("raw-token")).thenReturn("hash");
		when(userSessionMapper.findByRefreshTokenHash("hash")).thenReturn(Optional.empty());

		handler.handle(new LogoutCommand(userId, "raw-token", false));

		verify(userSessionMapper, never()).revoke(any(UUID.class), any(Instant.class), anyString());
	}
}
