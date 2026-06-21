package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.SendEmailVerificationCommand;
import com.soomgil.auth.application.service.EmailVerificationService;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증 메일을 발송한다.
 *
 * <p>이메일로 계정을 찾고, 이미 인증된 경우 발송하지 않는다. 토큰 발급/저장/발송은
 * {@link EmailVerificationService}에 위임한다. 계정이 존재하지 않아도 동일한 202
 * 응답을 반환해 계정 존재 여부를 노출하지 않는다.
 */
@Component
@Transactional
public class SendEmailVerificationCommandHandler implements CommandHandler<SendEmailVerificationCommand, NoResult> {

	private final EmailAddressMapper emailAddressMapper;
	private final EmailVerificationService emailVerificationService;

	public SendEmailVerificationCommandHandler(
		EmailAddressMapper emailAddressMapper,
		EmailVerificationService emailVerificationService
	) {
		this.emailAddressMapper = emailAddressMapper;
		this.emailVerificationService = emailVerificationService;
	}

	@Override
	public NoResult handle(SendEmailVerificationCommand command) {
		String normalizedEmail = EmailAddress.normalize(command.email());

		EmailAddress emailAddress = emailAddressMapper.findActiveByNormalizedEmail(normalizedEmail)
			.orElse(null);

		// 계정이 없거나 이미 인증된 경우 조용히 무시 (보안: 계정 존재 여부 노출 방지)
		if (emailAddress == null || emailAddress.verifiedAt() != null) {
			return NoResult.INSTANCE;
		}

		emailVerificationService.issueAndSend(emailAddress.id(), emailAddress.email());

		return NoResult.INSTANCE;
	}
}
