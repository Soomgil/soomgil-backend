package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.RefreshCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.UserSession;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * refresh token rotation을 처리한다.
 *
 * <p>refresh token hash로 session을 찾고, 유효하면 기존 session을 revoke하고
 * 같은 family로 새 refresh token을 발급한다.
 * 이미 revoke된 session의 token이 재사용되면 family 전체를 revoke하고 예외를 던진다.
 */
@Component
@Transactional
public class RefreshCommandHandler implements CommandHandler<RefreshCommand, AuthTokenResult> {

	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final UserProfileMapper userProfileMapper;
	private final UserSessionMapper userSessionMapper;
	private final AuthTokenService authTokenService;

	public RefreshCommandHandler(
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper,
		UserProfileMapper userProfileMapper,
		UserSessionMapper userSessionMapper,
		AuthTokenService authTokenService
	) {
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.userProfileMapper = userProfileMapper;
		this.userSessionMapper = userSessionMapper;
		this.authTokenService = authTokenService;
	}

	@Override
	public AuthTokenResult handle(RefreshCommand command) {
		String hash = authTokenService.hashRefreshToken(command.refreshToken());

		UserSession session = userSessionMapper.findByRefreshTokenHash(hash)
			.orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_INVALID));

		if (session.revokedAt() != null) {
			userSessionMapper.revokeFamily(
				session.refreshTokenFamilyId(), Instant.now(), "REUSE_DETECTED"
			);
			throw new AuthException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
		}

		if (session.expiresAt().isBefore(Instant.now())) {
			throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		AuthUser user = userMapper.findById(session.userId())
			.orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

		if (!user.canLogin()) {
			throw new AuthException(ErrorCode.USER_INACTIVE);
		}

		userSessionMapper.revoke(session.id(), Instant.now(), "ROTATED");

		AuthTokenService.IssuedRefreshToken newRefresh = authTokenService.mintRefreshToken();
		userSessionMapper.insert(
			UUID.randomUUID(),
			user.id(),
			newRefresh.hash(),
			session.refreshTokenFamilyId(),
			newRefresh.expiresAt()
		);

		EmailAddress emailAddress = emailAddressMapper.findPrimaryByUserId(user.id()).orElse(null);
		String email = emailAddress != null ? emailAddress.email() : null;
		String displayName = userProfileMapper.findDisplayName(user.id()).orElse(null);

		String accessToken = authTokenService.mintAccessToken(user.id(), email);

		boolean onboarded = user.status() == UserStatus.ACTIVE;
		return new AuthTokenResult(accessToken, newRefresh.raw(), user.id(), email, displayName, onboarded);
	}
}
