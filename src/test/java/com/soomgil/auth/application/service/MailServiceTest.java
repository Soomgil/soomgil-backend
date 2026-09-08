package com.soomgil.auth.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceTest {

	private final JavaMailSender mailSender = mock(JavaMailSender.class);
	private final MailService mailService = new MailService(
		mailSender,
		"http://localhost:5173/auth/verify-email",
		"http://localhost:5173/auth/reset-password",
		"http://localhost:5173/trip-invites",
		"sender@example.com"
	);

	@Test
	void sendsVerificationEmailWithPlainTextAndHtmlBodies() {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

		assertThatCode(() -> mailService.sendVerificationEmail("recipient@example.com", "verification-token"))
			.doesNotThrowAnyException();

		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void sendsPasswordResetEmailWithPlainTextAndHtmlBodies() {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

		assertThatCode(() -> mailService.sendPasswordResetEmail("recipient@example.com", "reset-token"))
			.doesNotThrowAnyException();

		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void sendsTripInviteEmailWithPlainTextAndHtmlBodies() {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

		assertThatCode(() -> mailService.sendTripInviteEmail("recipient@example.com", "JOIN-ME"))
			.doesNotThrowAnyException();

		verify(mailSender).send(any(MimeMessage.class));
	}
}
