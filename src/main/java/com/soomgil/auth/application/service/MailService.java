package com.soomgil.auth.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 이메일 발송을 담당한다.
 *
 * <p>Spring Boot의 {@link JavaMailSender}를 사용한다. 기본 local 환경에서는 Mailpit으로 전달되며,
 * SMTP 환경 변수를 설정하면 Gmail 같은 외부 메일 서버를 통해 발송한다.
 */
@Component
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final JavaMailSender mailSender;
	private final String verificationBaseUrl;
	private final String resetBaseUrl;
	private final String tripInviteBaseUrl;
	private final String fromAddress;

	public MailService(
		JavaMailSender mailSender,
		@Value("${soomgil.mail.verification-base-url:http://localhost:5173/auth/verify-email}") String verificationBaseUrl,
		@Value("${soomgil.mail.reset-base-url:http://localhost:5173/auth/reset-password}") String resetBaseUrl,
		@Value("${soomgil.mail.trip-invite-base-url:http://localhost:5173/trip-invites}") String tripInviteBaseUrl,
		@Value("${soomgil.mail.from:noreply@soomgil.com}") String fromAddress
	) {
		this.mailSender = mailSender;
		this.verificationBaseUrl = verificationBaseUrl;
		this.resetBaseUrl = resetBaseUrl;
		this.tripInviteBaseUrl = tripInviteBaseUrl;
		this.fromAddress = fromAddress;
	}

	/** 수신 동의가 확인된 사용자에게 여행 초대 메일을 발송한다. */
	public void sendTripInviteEmail(String toEmail, String inviteCode) {
		String link = tripInviteBaseUrl + "/" + URLEncoder.encode(inviteCode, StandardCharsets.UTF_8);
		String subject = "[숨길] 새로운 여행 초대가 도착했습니다";
		String plainText = "숨길 여행 초대\n\n아래 링크에서 여행 초대를 확인하세요.\n" + link;
		String html = """
			<!doctype html><html lang="ko"><body style="margin:0;padding:32px;background:#f5f7fb;font-family:Arial,sans-serif;color:#172033">
			<div style="max-width:560px;margin:0 auto;padding:32px;background:#fff;border:1px solid #e5e7eb;border-radius:16px">
			<p style="color:#6d5ce7;font-weight:700">숨길 SOOMGIL</p>
			<h1 style="font-size:24px">새로운 여행 초대가 도착했습니다</h1>
			<p style="line-height:1.7">아래 버튼을 눌러 여행 정보를 확인하고 초대를 수락할 수 있습니다.</p>
			<p><a href="%s" style="display:inline-block;padding:13px 22px;border-radius:10px;background:#6d5ce7;color:#fff;text-decoration:none;font-weight:700">여행 초대 확인하기</a></p>
			<p style="color:#64748b;font-size:12px">설정에서 여행 초대 이메일 수신을 언제든 끌 수 있습니다.</p>
			</div></body></html>
			""".formatted(link);
		sendHtml(toEmail, subject, plainText, html);
	}

	/**
	 * 이메일 인증 메일을 발송한다.
	 *
	 * @param toEmail 수신자 이메일
	 * @param rawToken 인증 토큰 (raw)
	 */
	public void sendVerificationEmail(String toEmail, String rawToken) {
		String link = verificationBaseUrl + "?token=" + rawToken;
		String subject = "[숨길] 이메일 주소를 확인해주세요";
		String plainText = """
			숨길 회원가입 이메일 확인

			아래 링크를 열어 이메일 주소를 확인해주세요.
			%s

			링크는 24시간 후 만료됩니다.
			본인이 요청하지 않았다면 이 메일을 무시해주세요.
			""".formatted(link);
		String html = """
			<!doctype html>
			<html lang="ko"><body style="margin:0;padding:32px;background:#f5f7fb;font-family:Arial,sans-serif;color:#172033">
			<div style="max-width:560px;margin:0 auto;padding:32px;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px">
			<p style="margin:0 0 12px;color:#6d5ce7;font-size:14px;font-weight:700">숨길 SOOMGIL</p>
			<h1 style="margin:0 0 16px;font-size:24px">이메일 주소를 확인해주세요</h1>
			<p style="margin:0 0 24px;line-height:1.7">회원가입을 마치려면 아래 버튼을 눌러 이메일 주소를 확인해주세요.</p>
			<p style="margin:0 0 24px"><a href="%s" style="display:inline-block;padding:13px 22px;border-radius:10px;background:#6d5ce7;color:#ffffff;text-decoration:none;font-weight:700">이메일 인증하기</a></p>
			<p style="margin:0;color:#64748b;font-size:13px;line-height:1.6">버튼이 열리지 않으면 아래 주소를 브라우저에 붙여 넣어주세요.<br><a href="%s" style="color:#4f46e5;word-break:break-all">%s</a></p>
			<hr style="margin:28px 0;border:0;border-top:1px solid #e5e7eb">
			<p style="margin:0;color:#64748b;font-size:12px;line-height:1.6">이 링크는 24시간 후 만료됩니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
			</div></body></html>
			""".formatted(link, link, link);

		sendHtml(toEmail, subject, plainText, html);
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
		String plainText = """
			숨길 비밀번호 재설정

			아래 링크를 열어 비밀번호를 재설정해주세요.
			%s

			링크는 1시간 후 만료됩니다.
			본인이 요청하지 않았다면 이 메일을 무시해주세요.
			""".formatted(link);
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

		sendHtml(toEmail, subject, plainText, html);
	}

	private void sendHtml(String to, String subject, String plainText, String html) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(plainText, html);
			helper.setFrom(fromAddress, "숨길");
			message.setHeader("Auto-Submitted", "auto-generated");
			mailSender.send(message);
			log.info("Email sent to: {} subject: {}", to, subject);
		} catch (MessagingException | UnsupportedEncodingException e) {
			log.error("Failed to send email to: {}", to, e);
			throw new IllegalStateException("Failed to send email", e);
		}
	}
}
