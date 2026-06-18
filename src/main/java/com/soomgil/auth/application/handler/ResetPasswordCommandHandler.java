package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.ResetPasswordCommand;
import com.soomgil.auth.application.service.PasswordHasher;
import com.soomgil.auth.application.service.TokenGenerator;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.PasswordResetToken;
import com.soomgil.auth.infrastructure.persistence.PasswordCredentialMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordResetTokenMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호를 재설정한다.
 *
 * <p>재설정 토큰을 검증하고, 새 비밀번호로 hash를 업데이트한다.
 * 비밀번호 변경 후 모든 세션을 폐기한다 (모든 기기에서 로그아웃).
 */
@Component
@Transactional
public class ResetPasswordCommandHandler implements CommandHandler<ResetPasswordCommand, NoResult> {

	private final PasswordResetTokenMapper passwordResetTokenMapper;
	private final PasswordCredentialMapper passwordCredentialMapper;
	private final UserSessionMapper userSessionMapper;
	private final PasswordHasher passwordHasher;
	private final TokenGenerator tokenGenerator;

	public ResetPasswordCommandHandler(
		PasswordResetTokenMapper passwordResetTokenMapper,
		PasswordCredentialMapper passwordCredentialMapper,
		UserSessionMapper userSessionMapper,
		PasswordHasher passwordHasher,
		TokenGenerator tokenGenerator
	) {
		this.passwordResetTokenMapper = passwordResetTokenMapper;
		this.passwordCredentialMapper = passwordCredentialMapper;
		this.userSessionMapper = userSessionMapper;
		this.passwordHasher = passwordHasher;
		this.tokenGenerator = tokenGenerator;
	}

	@Override
	public NoResult handle(ResetPasswordCommand command) {
		String hash = tokenGenerator.hash(command.token());

		PasswordResetToken token = passwordResetTokenMapper.findByTokenHash(hash)
			.orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));

		if (token.usedAt() != null) {
			throw new AuthException(ErrorCode.INVALID_TOKEN);
		}

		if (token.expiresAt().isBefore(Instant.now())) {
			throw new AuthException(ErrorCode.TOKEN_EXPIRED);
		}

		passwordResetTokenMapper.markUsed(token.id(), Instant.now());
		passwordCredentialMapper.updatePasswordHash(token.userId(), passwordHasher.hash(command.newPassword()));
		userSessionMapper.revokeAllForUser(token.userId(), Instant.now(), "PASSWORD_RESET");

		return NoResult.INSTANCE;
	}
}
