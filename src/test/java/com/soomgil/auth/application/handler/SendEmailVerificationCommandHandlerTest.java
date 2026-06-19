package com.soomgil.auth.application.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.command.SendEmailVerificationCommand;
import com.soomgil.auth.application.service.EmailVerificationService;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendEmailVerificationCommandHandlerTest {

	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);

	private final SendEmailVerificationCommandHandler handler = new SendEmailVerificationCommandHandler(
		emailAddressMapper, emailVerificationService
	);

	@Test
	@DisplayName("미인증 이메일이면 인증 서비스로 토큰 발급/발송을 위임한다")
	void sendsVerificationEmailForUnverifiedAddress() {
		UUID emailId = UUID.randomUUID();
		EmailAddress email = new EmailAddress(emailId, UUID.randomUUID(), "user@example.com", "user@example.com", true, null);

		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com")).thenReturn(Optional.of(email));

		handler.handle(new SendEmailVerificationCommand("USER@example.com"));

		verify(emailVerificationService).issueAndSend(emailId, "user@example.com");
	}

	@Test
	@DisplayName("이미 인증된 이메일은 메일을 발송하지 않고 조용히 NoResult를 반환한다")
	void doesNotSendForAlreadyVerifiedEmail() {
		EmailAddress verified = new EmailAddress(
			UUID.randomUUID(), UUID.randomUUID(), "user@example.com", "user@example.com", true, Instant.now()
		);
		when(emailAddressMapper.findActiveByNormalizedEmail(anyString())).thenReturn(Optional.of(verified));

		handler.handle(new SendEmailVerificationCommand("user@example.com"));

		verify(emailVerificationService, never()).issueAndSend(any(UUID.class), anyString());
	}

	@Test
	@DisplayName("존재하지 않는 이메일은 메일을 발송하지 않는다")
	void doesNotSendForUnknownEmail() {
		when(emailAddressMapper.findActiveByNormalizedEmail(anyString())).thenReturn(Optional.empty());

		handler.handle(new SendEmailVerificationCommand("unknown@example.com"));

		verify(emailVerificationService, never()).issueAndSend(any(UUID.class), anyString());
	}
}
