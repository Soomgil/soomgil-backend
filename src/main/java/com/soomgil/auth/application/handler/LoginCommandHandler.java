package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.LoginCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.application.service.PasswordHasher;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.PasswordCredential;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.PasswordCredentialMapper;
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
 * 이메일/비밀번호 로그인을 처리한다.
 *
 * <p>이메일로 계정을 찾고 bcrypt hash로 비밀번호를 검증한 뒤 access/refresh token을 발급한다.
 * 로그인 성공 시 failed_login_count를 reset하고 last_login_at을 갱신한다.
 */
@Component
@Transactional
public class LoginCommandHandler implements CommandHandler<LoginCommand, AuthTokenResult> {

	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final PasswordCredentialMapper passwordCredentialMapper;
	private final UserProfileMapper userProfileMapper;
	private final UserSessionMapper userSessionMapper;
	private final PasswordHasher passwordHasher;
	private final AuthTokenService authTokenService;

	public LoginCommandHandler(
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper,
		PasswordCredentialMapper passwordCredentialMapper,
		UserProfileMapper userProfileMapper,
		UserSessionMapper userSessionMapper,
		PasswordHasher passwordHasher,
		AuthTokenService authTokenService
	) {
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.passwordCredentialMapper = passwordCredentialMapper;
		this.userProfileMapper = userProfileMapper;
		this.userSessionMapper = userSessionMapper;
		this.passwordHasher = passwordHasher;
		this.authTokenService = authTokenService;
	}

	@Override
	public AuthTokenResult handle(LoginCommand command) {
		String normalizedEmail = EmailAddress.normalize(command.email());

		EmailAddress emailAddress = emailAddressMapper.findActiveByNormalizedEmail(normalizedEmail)
			.orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

		AuthUser user = userMapper.findById(emailAddress.userId())
			.orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

		if (user.status() == UserStatus.PENDING) {
			throw new AuthException(ErrorCode.EMAIL_NOT_VERIFIED);
		}

		if (!user.canLogin()) {
			throw new AuthException(ErrorCode.USER_INACTIVE);
		}

		PasswordCredential credential = passwordCredentialMapper.findByUserId(user.id())
			.orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

		if (credential.isLocked()) {
			throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
		}

		if (!passwordHasher.matches(command.password(), credential.passwordHash())) {
			throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
		}

		passwordCredentialMapper.resetFailedLoginCount(user.id());
		userMapper.updateLastLoginAt(user.id(), Instant.now());

		String displayName = userProfileMapper.findDisplayName(user.id()).orElse(null);

		String accessToken = authTokenService.mintAccessToken(user.id(), emailAddress.email());
		AuthTokenService.IssuedRefreshToken refresh = authTokenService.mintRefreshToken();
		userSessionMapper.insert(
			UUID.randomUUID(), user.id(), refresh.hash(), UUID.randomUUID(), refresh.expiresAt()
		);

		return new AuthTokenResult(accessToken, refresh.raw(), user.id(), emailAddress.email(), displayName);
	}
}
