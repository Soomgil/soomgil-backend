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

import com.soomgil.auth.application.command.VerifyEmailCommand;
import com.soomgil.auth.application.service.TokenGenerator;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.EmailVerificationToken;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.EmailVerificationTokenMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerifyEmailCommandHandlerTest {

	private final EmailVerificationTokenMapper emailVerificationTokenMapper = mock(EmailVerificationTokenMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final UserMapper userMapper = mock(UserMapper.class);
	private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);

	private final VerifyEmailCommandHandler handler = new VerifyEmailCommandHandler(
		emailVerificationTokenMapper, emailAddressMapper, userMapper, tokenGenerator
	);

	@Test
	@DisplayName("정상 토큰이면 verified_at을 업데이트하고 계정을 ACTIVE로 전환하며 userId를 반환한다")
	void verifiesEmailSuccessfully() {
		UUID emailId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		EmailVerificationToken token = new EmailVerificationToken(
			UUID.randomUUID(), emailId, "hash", Instant.now().plusSeconds(3600), null
		);
		EmailAddress email = new EmailAddress(emailId, userId, "user@example.com", "user@example.com", true, null);

		when(tokenGenerator.hash("raw-token")).thenReturn("hash");
		when(emailVerificationTokenMapper.findByTokenHash("hash")).thenReturn(Optional.of(token));
		when(emailAddressMapper.findById(emailId)).thenReturn(Optional.of(email));

		UUID result = handler.handle(new VerifyEmailCommand("raw-token"));

		assertThat(result).isEqualTo(userId);
		verify(emailVerificationTokenMapper).markUsed(eq(token.id()), any(Instant.class));
		verify(emailAddressMapper).updateVerifiedAt(eq(emailId), any(Instant.class));
		verify(userMapper).updateStatus(eq(userId), eq(UserStatus.ACTIVE.name()));
	}

	@Test
	@DisplayName("잘못된 토큰은 INVALID_TOKEN 예외를 던진다")
	void throwsWhenTokenNotFound() {
		when(tokenGenerator.hash(anyString())).thenReturn("hash");
		when(emailVerificationTokenMapper.findByTokenHash("hash")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new VerifyEmailCommand("raw-token")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_TOKEN);

		verify(emailAddressMapper, never()).updateVerifiedAt(any(UUID.class), any(Instant.class));
		verify(userMapper, never()).updateStatus(any(UUID.class), anyString());
	}

	@Test
	@DisplayName("이미 사용된 토큰은 INVALID_TOKEN 예외를 던진다")
	void throwsWhenTokenAlreadyUsed() {
		EmailVerificationToken used = new EmailVerificationToken(
			UUID.randomUUID(), UUID.randomUUID(), "hash", Instant.now().plusSeconds(3600), Instant.now()
		);
		when(tokenGenerator.hash("raw-token")).thenReturn("hash");
		when(emailVerificationTokenMapper.findByTokenHash("hash")).thenReturn(Optional.of(used));

		assertThatThrownBy(() -> handler.handle(new VerifyEmailCommand("raw-token")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	@DisplayName("만료된 토큰은 TOKEN_EXPIRED 예외를 던진다")
	void throwsWhenTokenExpired() {
		EmailVerificationToken expired = new EmailVerificationToken(
			UUID.randomUUID(), UUID.randomUUID(), "hash", Instant.now().minusSeconds(60), null
		);
		when(tokenGenerator.hash("raw-token")).thenReturn("hash");
		when(emailVerificationTokenMapper.findByTokenHash("hash")).thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> handler.handle(new VerifyEmailCommand("raw-token")))
			.isInstanceOf(AuthException.class)
			.extracting(e -> ((AuthException) e).errorCode())
			.isEqualTo(ErrorCode.TOKEN_EXPIRED);
	}
}
