package com.soomgil.auth.application.service;

import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.EmailVerificationTokenMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 토큰 발급/저장/발송을 캡슐화한다.
 *
 * <p>회원가입({@code RegisterCommandHandler})과 인증 메일 재발송
 * ({@code SendEmailVerificationCommandHandler}) 양쪽에서 재사용된다.
 * 토큰은 {@link TokenGenerator}로 발급하여 SHA-256 hash를 DB에 저장하고,
 * raw 토큰을 {@link MailService}로 발송한다. 유효기간은 24시간이다.
 */
@Component
public class EmailVerificationService {

	/** 인증 토큰 유효기간 (초). 24시간. */
	public static final long TOKEN_TTL_SECONDS = 86_400;

	private final TokenGenerator tokenGenerator;
	private final EmailVerificationTokenMapper tokenMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final MailService mailService;

	public EmailVerificationService(
		TokenGenerator tokenGenerator,
		EmailVerificationTokenMapper tokenMapper,
		EmailAddressMapper emailAddressMapper,
		MailService mailService
	) {
		this.tokenGenerator = tokenGenerator;
		this.tokenMapper = tokenMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.mailService = mailService;
	}

	/**
	 * 인증 토큰을 발급하고 메일을 발송한다.
	 *
	 * @param emailAddressId 대상 {@code auth.user_email_addresses} row의 id
	 * @param email 수신자 이메일 (raw 토큰이 포함된 링크를 받을 주소)
	 */
	public void issueAndSend(UUID emailAddressId, String email) {
		TokenGenerator.GeneratedToken token = tokenGenerator.generate();
		Instant expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS);

		tokenMapper.insert(emailAddressId, token.hash(), expiresAt);
		emailAddressMapper.updateVerificationLastSentAt(emailAddressId, Instant.now());

		mailService.sendVerificationEmail(email, token.raw());
	}
}
