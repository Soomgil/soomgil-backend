package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.OnboardCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserPolicyAcceptanceMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 신규 가입자의 온보딩(약관동의 및 닉네임 설정)을 처리한다.
 */
@Component
@Transactional
public class OnboardCommandHandler implements CommandHandler<OnboardCommand, AuthTokenResult> {

	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final UserProfileMapper userProfileMapper;
	private final UserPolicyAcceptanceMapper userPolicyAcceptanceMapper;
	private final UserSessionMapper userSessionMapper;
	private final AuthTokenService authTokenService;

	public OnboardCommandHandler(
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper,
		UserProfileMapper userProfileMapper,
		UserPolicyAcceptanceMapper userPolicyAcceptanceMapper,
		UserSessionMapper userSessionMapper,
		AuthTokenService authTokenService
	) {
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.userProfileMapper = userProfileMapper;
		this.userPolicyAcceptanceMapper = userPolicyAcceptanceMapper;
		this.userSessionMapper = userSessionMapper;
		this.authTokenService = authTokenService;
	}

	@Override
	public AuthTokenResult handle(OnboardCommand command) {
		AuthUser user = userMapper.findById(command.userId())
			.orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

		if (user.status() != UserStatus.PENDING_ONBOARDING) {
			throw new AuthException(ErrorCode.CONFLICT);
		}

		// 1. 닉네임 유효성 검증 및 업데이트
		String displayName = command.displayName();
		if (displayName == null || displayName.isBlank()) {
			throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
		}
		userProfileMapper.updateDisplayName(user.id(), displayName);

		// 2. 약관 동의 저장
		if (command.acceptedPolicyDocumentIds() != null) {
			for (UUID policyId : command.acceptedPolicyDocumentIds()) {
				userPolicyAcceptanceMapper.insert(user.id(), policyId);
			}
		}

		// 3. 유저 상태 ACTIVE로 변경
		userMapper.updateStatus(user.id(), UserStatus.ACTIVE.name());

		// 4. 새 토큰 발행
		EmailAddress emailAddress = emailAddressMapper.findPrimaryByUserId(user.id()).orElse(null);
		String email = emailAddress != null ? emailAddress.email() : null;

		String accessToken = authTokenService.mintAccessToken(user.id(), email);
		AuthTokenService.IssuedRefreshToken refresh = authTokenService.mintRefreshToken();
		userSessionMapper.insert(
			UUID.randomUUID(), user.id(), refresh.hash(), UUID.randomUUID(), refresh.expiresAt()
		);

		return new AuthTokenResult(accessToken, refresh.raw(), user.id(), email, displayName, true);
	}
}
