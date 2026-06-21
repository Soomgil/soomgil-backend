package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.VerifyEmailCommand;
import com.soomgil.auth.application.service.TokenGenerator;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.EmailVerificationToken;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailVerificationTokenMapper;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증 토큰을 검증하고 verified_at을 업데이트하며, 계정을 {@code ACTIVE}로 전환한다.
 */
@Component
@Transactional
public class VerifyEmailCommandHandler implements CommandHandler<VerifyEmailCommand, UUID> {

	private final EmailVerificationTokenMapper emailVerificationTokenMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final UserMapper userMapper;
	private final TokenGenerator tokenGenerator;

	public VerifyEmailCommandHandler(
		EmailVerificationTokenMapper emailVerificationTokenMapper,
		EmailAddressMapper emailAddressMapper,
		UserMapper userMapper,
		TokenGenerator tokenGenerator
	) {
		this.emailVerificationTokenMapper = emailVerificationTokenMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.userMapper = userMapper;
		this.tokenGenerator = tokenGenerator;
	}

	@Override
	public UUID handle(VerifyEmailCommand command) {
		String hash = tokenGenerator.hash(command.token());

		EmailVerificationToken token = emailVerificationTokenMapper.findByTokenHash(hash)
			.orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));

		if (token.usedAt() != null) {
			throw new AuthException(ErrorCode.INVALID_TOKEN);
		}

		if (token.expiresAt().isBefore(Instant.now())) {
			throw new AuthException(ErrorCode.TOKEN_EXPIRED);
		}

		emailVerificationTokenMapper.markUsed(token.id(), Instant.now());
		emailAddressMapper.updateVerifiedAt(token.emailAddressId(), Instant.now());

		UUID userId = emailAddressMapper.findById(token.emailAddressId())
			.map(EmailAddress::userId)
			.orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
		userMapper.updateStatus(userId, UserStatus.ACTIVE.name());
		return userId;
	}
}
