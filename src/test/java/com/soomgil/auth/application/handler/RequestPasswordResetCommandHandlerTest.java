package com.soomgil.auth.application.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.command.RequestPasswordResetCommand;
import com.soomgil.auth.application.service.MailService;
import com.soomgil.auth.application.service.TokenGenerator;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordResetTokenMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestPasswordResetCommandHandlerTest {

	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final PasswordResetTokenMapper passwordResetTokenMapper = mock(PasswordResetTokenMapper.class);
	private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);
	private final MailService mailService = mock(MailService.class);

	private final RequestPasswordResetCommandHandler handler = new RequestPasswordResetCommandHandler(
		emailAddressMapper, passwordResetTokenMapper, tokenGenerator, mailService
	);

	@Test
	@DisplayName("계정이 존재하면 재설정 토큰을 생성하고 메일을 발송한다")
	void sendsResetEmailForExistingAccount() {
		UUID userId = UUID.randomUUID();
		EmailAddress email = new EmailAddress(UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, null);

		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com")).thenReturn(Optional.of(email));
		when(tokenGenerator.generate()).thenReturn(new TokenGenerator.GeneratedToken("raw-token", "token-hash"));

		handler.handle(new RequestPasswordResetCommand("USER@example.com"));

		verify(passwordResetTokenMapper).insert(eq(userId), eq("token-hash"), any(Instant.class));
		verify(mailService).sendPasswordResetEmail("user@example.com", "raw-token");
	}

	@Test
	@DisplayName("존재하지 않는 계정은 메일을 발송하지 않고 조용히 성공 응답을 반환한다 (계정 노출 방지)")
	void doesNotSendForUnknownAccount() {
		when(emailAddressMapper.findActiveByNormalizedEmail(anyString())).thenReturn(Optional.empty());

		handler.handle(new RequestPasswordResetCommand("unknown@example.com"));

		verify(tokenGenerator, never()).generate();
		verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
		verify(passwordResetTokenMapper, never()).insert(any(UUID.class), anyString(), any(Instant.class));
	}
}
