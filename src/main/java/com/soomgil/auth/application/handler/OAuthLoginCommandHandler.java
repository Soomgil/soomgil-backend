package com.soomgil.auth.application.handler;

import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.OAuthLoginCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.application.service.OAuthClient;
import com.soomgil.auth.domain.model.AuthUser;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.OAuthIdentity;
import com.soomgil.auth.domain.model.UserStatus;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.OAuthIdentityMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.auth.infrastructure.persistence.UserSettingsMapper;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 로그인을 처리한다.
 *
 * <p>authorization code로 제공자 사용자 정보를 조회하고:
 * 1. 기존 OAuth 연결이 있으면 → 로그인
 * 2. 기존 이메일 계정이 있으면 → OAuth identity 연결
 * 3. 둘 다 없으면 → 신규 가입 (users, email, profile, settings, oauth identity 생성)
 * 그 후 access/refresh token을 발급한다.
 */
@Component
@Transactional
public class OAuthLoginCommandHandler implements CommandHandler<OAuthLoginCommand, AuthTokenResult> {

	private static final short KAKAO_PROVIDER_ID = 2;
	private static final short GOOGLE_PROVIDER_ID = 3;

	private final OAuthClient oauthClient;
	private final OAuthIdentityMapper oauthIdentityMapper;
	private final UserMapper userMapper;
	private final EmailAddressMapper emailAddressMapper;
	private final UserProfileMapper userProfileMapper;
	private final UserSettingsMapper userSettingsMapper;
	private final UserSessionMapper userSessionMapper;
	private final AuthTokenService authTokenService;

	public OAuthLoginCommandHandler(
		OAuthClient oauthClient,
		OAuthIdentityMapper oauthIdentityMapper,
		UserMapper userMapper,
		EmailAddressMapper emailAddressMapper,
		UserProfileMapper userProfileMapper,
		UserSettingsMapper userSettingsMapper,
		UserSessionMapper userSessionMapper,
		AuthTokenService authTokenService
	) {
		this.oauthClient = oauthClient;
		this.oauthIdentityMapper = oauthIdentityMapper;
		this.userMapper = userMapper;
		this.emailAddressMapper = emailAddressMapper;
		this.userProfileMapper = userProfileMapper;
		this.userSettingsMapper = userSettingsMapper;
		this.userSessionMapper = userSessionMapper;
		this.authTokenService = authTokenService;
	}

	@Override
	public AuthTokenResult handle(OAuthLoginCommand command) {
		OAuthClient.ProviderUserInfo providerUser = oauthClient.getUserInfo(
			command.provider(), command.code(), command.redirectUri()
		);

		short providerId = command.provider().name().equals("KAKAO") ? KAKAO_PROVIDER_ID : GOOGLE_PROVIDER_ID;

		// 1. 기존 OAuth 연결 확인
		OAuthIdentity existing = oauthIdentityMapper.findByProviderAndSubject(
			providerId, providerUser.providerSubject()
		).orElse(null);

		UUID userId;
		String email;
		String displayName;

		if (existing != null) {
			// 기존 연결 → 로그인
			userId = existing.userId();
			oauthIdentityMapper.updateLastLoginAt(existing.id(), Instant.now());
			userMapper.updateLastLoginAt(userId, Instant.now());

			EmailAddress emailAddress = emailAddressMapper.findPrimaryByUserId(userId).orElse(null);
			email = emailAddress != null ? emailAddress.email() : providerUser.email();
			displayName = userProfileMapper.findDisplayName(userId).orElse(providerUser.displayName());
		} else if (providerUser.email() != null) {
			// 2. 이메일로 기존 계정 확인
			String normalizedEmail = EmailAddress.normalize(providerUser.email());
			EmailAddress existingEmail = emailAddressMapper.findActiveByNormalizedEmail(normalizedEmail).orElse(null);

			if (existingEmail != null) {
				// 기존 계정에 OAuth 연결
				userId = existingEmail.userId();
				displayName = userProfileMapper.findDisplayName(userId).orElse(providerUser.displayName());
				email = existingEmail.email();
			} else {
				// 3. 신규 가입
				userId = createNewOAuthUser(providerUser, providerId);
				displayName = fallbackDisplayName(providerUser);
				email = providerUser.email();
			}
		} else {
			// 이메일 없는 신규 가입
			userId = createNewOAuthUser(providerUser, providerId);
			displayName = fallbackDisplayName(providerUser);
			email = null;
		}

		// 토큰 발급
		String accessToken = authTokenService.mintAccessToken(userId, email);
		AuthTokenService.IssuedRefreshToken refresh = authTokenService.mintRefreshToken();
		userSessionMapper.insert(
			UUID.randomUUID(), userId, refresh.hash(), UUID.randomUUID(), refresh.expiresAt()
		);

		boolean onboarded = userMapper.findById(userId)
			.map(u -> u.status() == UserStatus.ACTIVE)
			.orElse(false);

		return new AuthTokenResult(accessToken, refresh.raw(), userId, email, displayName, onboarded);
	}

	private UUID createNewOAuthUser(OAuthClient.ProviderUserInfo providerUser, short providerId) {
		UUID userId = UUID.randomUUID();
		userMapper.insert(userId, UserStatus.PENDING_ONBOARDING.name());

		if (providerUser.email() != null) {
			String normalizedEmail = EmailAddress.normalize(providerUser.email());
			// OAuth 제공자가 이메일을 검증했으므로 verifiedAt을 now로 설정한다.
			emailAddressMapper.insertPrimary(userId, providerUser.email(), normalizedEmail, Instant.now());
		}

		String name = fallbackDisplayName(providerUser);
		userProfileMapper.insert(userId, name);
		userSettingsMapper.insertDefaults(userId);

		oauthIdentityMapper.insert(
			userId,
			providerId,
			providerUser.providerSubject(),
			providerUser.email(),
			providerUser.displayName()
		);

		return userId;
	}

	/**
	 * provider가 알려준 displayName이 있으면 그대로 쓰고, 없으면 email의 로컬 파트를
	 * fallback으로 사용한다. 둘 다 없으면 카카오 회원번호 앞 8자리를 임시 이름으로 쓴다.
	 * 사용자는 가입 후 PATCH /me로 언제든 원하는 이름으로 바꿀 수 있다.
	 */
	private String fallbackDisplayName(OAuthClient.ProviderUserInfo providerUser) {
		if (providerUser.displayName() != null && !providerUser.displayName().isBlank()) {
			return providerUser.displayName();
		}
		if (providerUser.email() != null) {
			String localPart = providerUser.email().split("@", 2)[0];
			if (!localPart.isBlank()) {
				return localPart;
			}
		}
		String subject = providerUser.providerSubject();
		return subject != null && subject.length() >= 8 ? subject.substring(0, 8) : "사용자";
	}
}
