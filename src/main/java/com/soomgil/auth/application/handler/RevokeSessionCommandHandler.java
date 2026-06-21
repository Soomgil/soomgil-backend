package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.RevokeSessionCommand;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.UserSession;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 특정 세션을 폐기한다. 본인 세션만 폐기 가능하다.
 */
@Component
@Transactional
public class RevokeSessionCommandHandler implements CommandHandler<RevokeSessionCommand, NoResult> {

	private final UserSessionMapper userSessionMapper;

	public RevokeSessionCommandHandler(UserSessionMapper userSessionMapper) {
		this.userSessionMapper = userSessionMapper;
	}

	@Override
	public NoResult handle(RevokeSessionCommand command) {
		UserSession session = userSessionMapper.findByIdAndUserId(command.sessionId(), command.userId())
			.orElseThrow(() -> new AuthException(ErrorCode.SESSION_NOT_FOUND));

		if (session.revokedAt() == null) {
			userSessionMapper.revoke(session.id(), Instant.now(), "USER_REVOKE");
		}

		return NoResult.INSTANCE;
	}
}
