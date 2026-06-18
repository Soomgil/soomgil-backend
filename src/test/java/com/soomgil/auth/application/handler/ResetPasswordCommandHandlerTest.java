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

import com.soomgil.auth.application.command.ResetPasswordCommand;
import com.soomgil.auth.application.service.PasswordHasher;
import com.soomgil.auth.application.service.TokenGenerator;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.PasswordResetToken;
import com.soomgil.auth.infrastructure.persistence.PasswordCredentialMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordResetTokenMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResetPasswordCommandHandlerTest {

	private final PasswordResetTokenMapper passwordResetTokenMapper = mock(PasswordResetTokenMapper.class);
	private final PasswordCredentialMapper passwordCredentialMapper = mock(PasswordCredentialMapper.class);
	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);
	private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
	private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);

	private final ResetPasswordCommandHandler handler = new ResetPasswordCommandHandler(
		passwordResetTokenMapper, passwordCredentialMapper, userSessionMapper, passwordHasher, tokenGenerator
	);

	@Test
	@DisplayName("정상 토큰이면 비밀번호를 변경하고 모든 세션을 폐기한다")
	void resetsPasswordSuccessfully() {
		UUID userId = UUID.randomUUID();
		PasswordResetToken token = new PasswordResetToken(
			UUID.randomUUID(), userId, "hash", Instant.now().plusSeconds(600), null
		);

		when(tokenGenerator.hash("raw-token")).thenReturn("hash");
		when(passwordResetTokenMapper.findByTokenHash("hash")).thenReturn(Optional.of(token));
		when(passwordHasher.hash("newPassword")).thenReturn("new-hash");

		NoResult result = handler.handle(new ResetPasswordCommand("raw-token", "newPassword"));

		assertThat(result).isNotNull();
		verify(passwordResetTokenMapper).markUsed(eq(token.id()), any(Instant.class));
		verify(passwordCredentialMapper).updatePasswordHash(userId, "new-hash");
		verify(userSessionMapper).revokeAllForUser(eq(userId), any(Instant.class), eq("PASSWORD_RESET"));
	}

	@Test
	@DisplayName("잘못된 토큰은 INVALID_TOKEN 예외를 던진다")
	void throwsWhenTokenNotFound() {
		when(tokenGenerator.hash(anyString())).thenReturn("hash");
		when(passwordResetTokenMapper.findByTokenHash("hash")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new ResetPasswordCommand("raw-token", "newPassword")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_TOKEN);

		verify(passwordCredentialMapper, never()).updatePasswordHash(any(UUID.class), anyString());
		verify(userSessionMapper, never()).revokeAllForUser(any(UUID.class), any(Instant.class), anyString());
	}

	@Test
	@DisplayName("이미 사용된 토큰은 INVALID_TOKEN 예외를 던진다")
	void throwsWhenTokenAlreadyUsed() {
		PasswordResetToken used = new PasswordResetToken(
			UUID.randomUUID(), UUID.randomUUID(), "hash", Instant.now().plusSeconds(600), Instant.now()
		);
		when(tokenGenerator.hash("raw-token")).thenReturn("hash");
		when(passwordResetTokenMapper.findByTokenHash("hash")).thenReturn(Optional.of(used));

		assertThatThrownBy(() -> handler.handle(new ResetPasswordCommand("raw-token", "newPassword")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	@DisplayName("만료된 토큰은 TOKEN_EXPIRED 예외를 던진다")
	void throwsWhenTokenExpired() {
		PasswordResetToken expired = new PasswordResetToken(
			UUID.randomUUID(), UUID.randomUUID(), "hash", Instant.now().minusSeconds(60), null
		);
		when(tokenGenerator.hash("raw-token")).thenReturn("hash");
		when(passwordResetTokenMapper.findByTokenHash("hash")).thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> handler.handle(new ResetPasswordCommand("raw-token", "newPassword")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.TOKEN_EXPIRED);
	}
}
