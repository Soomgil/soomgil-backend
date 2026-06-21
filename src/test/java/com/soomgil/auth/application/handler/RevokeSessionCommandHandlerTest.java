package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.command.RevokeSessionCommand;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.UserSession;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevokeSessionCommandHandlerTest {

	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);

	private final RevokeSessionCommandHandler handler = new RevokeSessionCommandHandler(userSessionMapper);

	@Test
	@DisplayName("본인 세션을 정상적으로 revoke한다")
	void revokesOwnSession() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UserSession session = new UserSession(
			sessionId, userId, "hash", UUID.randomUUID(),
			Instant.now().plusSeconds(3600), null, Instant.now()
		);
		when(userSessionMapper.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

		handler.handle(new RevokeSessionCommand(sessionId, userId));

		verify(userSessionMapper).revoke(eq(sessionId), any(Instant.class), eq("USER_REVOKE"));
	}

	@Test
	@DisplayName("이미 revoke된 세션은 revoke를 중복 호출하지 않는다")
	void doesNotRevokeAlreadyRevokedSession() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UserSession revoked = new UserSession(
			sessionId, userId, "hash", UUID.randomUUID(),
			Instant.now().plusSeconds(3600), Instant.now().minusSeconds(60), Instant.now()
		);
		when(userSessionMapper.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(revoked));

		handler.handle(new RevokeSessionCommand(sessionId, userId));

		verify(userSessionMapper, never()).revoke(any(UUID.class), any(Instant.class), anyString());
	}

	@Test
	@DisplayName("타인의 세션이거나 존재하지 않는 세션은 SESSION_NOT_FOUND 예외를 던진다")
	void throwsWhenSessionNotFoundForUser() {
		UUID sessionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(userSessionMapper.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new RevokeSessionCommand(sessionId, userId)))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.SESSION_NOT_FOUND);
	}
}
