package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.RequestPasswordResetCommand;
import com.soomgil.auth.application.service.MailService;
import com.soomgil.auth.application.service.TokenGenerator;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordResetTokenMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 재설정을 요청한다.
 *
 * <p>계정 존재 여부와 무관하게 항상 성공 응답을 반환한다 (보안).
 * 계정이 존재하면 재설정 토큰을 생성하고 이메일로 발송한다.
 * 토큰 유효기간은 1시간이다.
 */
@Component
@Transactional
public class RequestPasswordResetCommandHandler implements CommandHandler<RequestPasswordResetCommand, NoResult> {

	private static final long RESET_TOKEN_TTL_SECONDS = 3_600; // 1시간

	private final EmailAddressMapper emailAddressMapper;
	private final PasswordResetTokenMapper passwordResetTokenMapper;
	private final TokenGenerator tokenGenerator;
	private final MailService mailService;

	public RequestPasswordResetCommandHandler(
		EmailAddressMapper emailAddressMapper,
		PasswordResetTokenMapper passwordResetTokenMapper,
		TokenGenerator tokenGenerator,
		MailService mailService
	) {
		this.emailAddressMapper = emailAddressMapper;
		this.passwordResetTokenMapper = passwordResetTokenMapper;
		this.tokenGenerator = tokenGenerator;
		this.mailService = mailService;
	}

	@Override
	public NoResult handle(RequestPasswordResetCommand command) {
		String normalizedEmail = EmailAddress.normalize(command.email());

		EmailAddress emailAddress = emailAddressMapper.findActiveByNormalizedEmail(normalizedEmail)
			.orElse(null);

		// 계정이 없으면 조용히 무시
		if (emailAddress == null) {
			return NoResult.INSTANCE;
		}

		TokenGenerator.GeneratedToken token = tokenGenerator.generate();
		Instant expiresAt = Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS);

		passwordResetTokenMapper.insert(emailAddress.userId(), token.hash(), expiresAt);
		mailService.sendPasswordResetEmail(emailAddress.email(), token.raw());

		return NoResult.INSTANCE;
	}
}
