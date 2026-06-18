package com.soomgil.auth.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 이메일 발송을 담당한다.
 *
 * <p>Spring Boot의 {@link JavaMailSender}를 사용하며, local 개발에서는 Mailpit (localhost:1025)로 메일이 전달된다.
 * Mailpit web UI (localhost:8025)에서 발송된 메일을 확인할 수 있다.
 */
@Component
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final String verificationBaseUrl;
	private final String resetBaseUrl;
	private final String fromAddress;

	public MailService(
		JavaMailSender mailSender,
		@Value("${soomgil.mail.verification-base-url:http://localhost:5173/auth/verify-email}") String verificationBaseUrl,
		@Value("${soomgil.mail.reset-base-url:http://localhost:5173/auth/reset-password}") String resetBaseUrl,
		@Value("${soomgil.mail.from:noreply@soomgil.com}") String fromAddress
	) {
		this.mailSender = mailSender;
		this.verificationBaseUrl = verificationBaseUrl;
		this.resetBaseUrl = resetBaseUrl;
		this.fromAddress = fromAddress;
	}

	/**
	 * 이메일 인증 메일을 발송한다.
	 *
	 * @param toEmail 수신자 이메일
	 * @param rawToken 인증 토큰 (raw)
	 */
	public void sendVerificationEmail(String toEmail, String rawToken) {
		String link = verificationBaseUrl + "?token=" + rawToken;
		String subject = "[숨길] 이메일 인증을 완료해주세요";
		String html = """
			<html><body>
			<h2>이메일 인증</h2>
			<p>아래 링크를 클릭하여 이메일 인증을 완료해주세요.</p>
			<p><a href="%s">이메일 인증하기</a></p>
			<p>또는 다음 토큰을 입력하세요: <code>%s</code></p>
			<p>이 링크는 24시간 후 만료됩니다.</p>
			</body></html>
			""".formatted(link, rawToken);

		sendHtml(toEmail, subject, html);
	}

	/**
	 * 비밀번호 재설정 메일을 발송한다.
	 *
	 * @param toEmail 수신자 이메일
	 * @param rawToken 재설정 토큰 (raw)
	 */
	public void sendPasswordResetEmail(String toEmail, String rawToken) {
		String link = resetBaseUrl + "?token=" + rawToken;
		String subject = "[숨길] 비밀번호 재설정";
		String html = """
			<html><body>
			<h2>비밀번호 재설정</h2>
			<p>비밀번호 재설정 요청을 받았습니다. 아래 링크를 클릭하여 비밀번호를 재설정하세요.</p>
			<p><a href="%s">비밀번호 재설정하기</a></p>
			<p>또는 다음 토큰을 입력하세요: <code>%s</code></p>
			<p>이 링크는 1시간 후 만료됩니다.</p>
			<p>본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.</p>
			</body></html>
			""".formatted(link, rawToken);

		sendHtml(toEmail, subject, html);
	}

	private void sendHtml(String to, String subject, String html) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			helper.setFrom(fromAddress);
			mailSender.send(message);
			log.info("Email sent to: {} subject: {}", to, subject);
		} catch (MessagingException e) {
			log.error("Failed to send email to: {}", to, e);
			throw new IllegalStateException("Failed to send email", e);
		}
	}
}
