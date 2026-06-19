package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.LogoutCommand;
import com.soomgil.auth.application.service.AuthTokenService;
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
 * 로그아웃을 처리한다.
 *
 * <p>allDevices=true면 사용자의 모든 활성 세션을 revoke한다.
 * 아니면 refreshToken에 해당하는 세션만 revoke한다.
 */
@Component
@Transactional
public class LogoutCommandHandler implements CommandHandler<LogoutCommand, NoResult> {

	private final UserSessionMapper userSessionMapper;
	private final AuthTokenService authTokenService;

	public LogoutCommandHandler(UserSessionMapper userSessionMapper, AuthTokenService authTokenService) {
		this.userSessionMapper = userSessionMapper;
		this.authTokenService = authTokenService;
	}

	@Override
	public NoResult handle(LogoutCommand command) {
		if (command.allDevices()) {
			userSessionMapper.revokeAllForUser(command.userId(), Instant.now(), "USER_LOGOUT_ALL");
			return NoResult.INSTANCE;
		}

		if (command.refreshToken() == null || command.refreshToken().isBlank()) {
			return NoResult.INSTANCE;
		}

		String hash = authTokenService.hashRefreshToken(command.refreshToken());
		userSessionMapper.findByRefreshTokenHash(hash)
			.ifPresent(session -> userSessionMapper.revoke(session.id(), Instant.now(), "USER_LOGOUT"));

		return NoResult.INSTANCE;
	}
}
