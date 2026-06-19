package com.soomgil.auth.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.api.dto.OAuthProviderCode;
import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.OAuthLoginCommand;
import com.soomgil.auth.application.service.AuthTokenService;
import com.soomgil.auth.application.service.OAuthClient;
import com.soomgil.auth.domain.model.EmailAddress;
import com.soomgil.auth.domain.model.OAuthIdentity;
import com.soomgil.auth.infrastructure.persistence.EmailAddressMapper;
import com.soomgil.auth.infrastructure.persistence.OAuthIdentityMapper;
import com.soomgil.auth.infrastructure.persistence.UserMapper;
import com.soomgil.auth.infrastructure.persistence.UserProfileMapper;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.auth.infrastructure.persistence.UserSettingsMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthLoginCommandHandlerTest {

	private final OAuthClient oauthClient = mock(OAuthClient.class);
	private final OAuthIdentityMapper oauthIdentityMapper = mock(OAuthIdentityMapper.class);
	private final UserMapper userMapper = mock(UserMapper.class);
	private final EmailAddressMapper emailAddressMapper = mock(EmailAddressMapper.class);
	private final UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
	private final UserSettingsMapper userSettingsMapper = mock(UserSettingsMapper.class);
	private final UserSessionMapper userSessionMapper = mock(UserSessionMapper.class);
	private final AuthTokenService authTokenService = mock(AuthTokenService.class);

	private final OAuthLoginCommandHandler handler = new OAuthLoginCommandHandler(
		oauthClient, oauthIdentityMapper, userMapper, emailAddressMapper,
		userProfileMapper, userSettingsMapper, userSessionMapper, authTokenService
	);

	private void stubTokenIssuance(UUID userId, String email) {
		when(authTokenService.mintAccessToken(userId, email)).thenReturn("access-token");
		when(authTokenService.mintRefreshToken()).thenReturn(
			new AuthTokenService.IssuedRefreshToken("raw-refresh", "hash-refresh", Instant.now().plusSeconds(3600))
		);
	}

	@Test
	@DisplayName("기존 OAuth 연결이 있으면 last_login_at을 갱신하고 토큰을 발급한다")
	void logsInWithExistingOAuthIdentity() {
		UUID userId = UUID.randomUUID();
		UUID identityId = UUID.randomUUID();
		OAuthIdentity existing = new OAuthIdentity(
			identityId, userId, (short) 2, "kakao-123", "user@example.com", "민지", Instant.now(), null
		);
		EmailAddress email = new EmailAddress(UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, null);

		when(oauthClient.getUserInfo(OAuthProviderCode.KAKAO, "auth-code", "https://redirect"))
			.thenReturn(new OAuthClient.ProviderUserInfo("kakao-123", "user@example.com", "민지"));
		when(oauthIdentityMapper.findByProviderAndSubject((short) 2, "kakao-123")).thenReturn(Optional.of(existing));
		when(emailAddressMapper.findPrimaryByUserId(userId)).thenReturn(Optional.of(email));
		when(userProfileMapper.findDisplayName(userId)).thenReturn(Optional.of("민지"));
		stubTokenIssuance(userId, "user@example.com");

		AuthTokenResult result = handler.handle(new OAuthLoginCommand(OAuthProviderCode.KAKAO, "auth-code", "https://redirect"));

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.email()).isEqualTo("user@example.com");
		assertThat(result.displayName()).isEqualTo("민지");
		verify(oauthIdentityMapper).updateLastLoginAt(eq(identityId), any(Instant.class));
		verify(userMapper).updateLastLoginAt(eq(userId), any(Instant.class));
		verify(userMapper, never()).insert(any(UUID.class), anyString());
	}

	@Test
	@DisplayName("이메일로 기존 계정이 있으면 OAuth identity를 연결하고 토큰을 발급한다")
	void linksOAuthIdentityToExistingEmailAccount() {
		UUID userId = UUID.randomUUID();
		EmailAddress existingEmail = new EmailAddress(UUID.randomUUID(), userId, "user@example.com", "user@example.com", true, null);

		when(oauthClient.getUserInfo(eq(OAuthProviderCode.GOOGLE), anyString(), anyString()))
			.thenReturn(new OAuthClient.ProviderUserInfo("google-456", "user@example.com", "민지"));
		when(oauthIdentityMapper.findByProviderAndSubject((short) 3, "google-456")).thenReturn(Optional.empty());
		when(emailAddressMapper.findActiveByNormalizedEmail("user@example.com")).thenReturn(Optional.of(existingEmail));
		when(userProfileMapper.findDisplayName(userId)).thenReturn(Optional.empty());
		stubTokenIssuance(userId, "user@example.com");

		AuthTokenResult result = handler.handle(new OAuthLoginCommand(OAuthProviderCode.GOOGLE, "code", "https://redirect"));

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.email()).isEqualTo("user@example.com");
		assertThat(result.displayName()).isEqualTo("민지");
		verify(oauthIdentityMapper, never()).updateLastLoginAt(any(UUID.class), any(Instant.class));
	}

	@Test
	@DisplayName("기존 계정이 없으면 신규 사용자를 생성하고 토큰을 발급한다")
	void createsNewUserWhenNoExistingAccount() {
		when(oauthClient.getUserInfo(eq(OAuthProviderCode.KAKAO), anyString(), anyString()))
			.thenReturn(new OAuthClient.ProviderUserInfo("kakao-new", "new@example.com", "새이름"));
		when(oauthIdentityMapper.findByProviderAndSubject((short) 2, "kakao-new")).thenReturn(Optional.empty());
		when(emailAddressMapper.findActiveByNormalizedEmail("new@example.com")).thenReturn(Optional.empty());
		when(authTokenService.mintAccessToken(any(UUID.class), anyString())).thenReturn("access-token");
		when(authTokenService.mintRefreshToken()).thenReturn(
			new AuthTokenService.IssuedRefreshToken("raw-refresh", "hash-refresh", Instant.now().plusSeconds(3600))
		);

		AuthTokenResult result = handler.handle(new OAuthLoginCommand(OAuthProviderCode.KAKAO, "code", "https://redirect"));

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("raw-refresh");
		assertThat(result.email()).isEqualTo("new@example.com");
		assertThat(result.displayName()).isEqualTo("새이름");

		verify(userMapper).insert(any(UUID.class), eq("ACTIVE"));
		verify(emailAddressMapper).insertPrimary(
			any(UUID.class), eq("new@example.com"), eq("new@example.com"), any(Instant.class)
		);
		verify(userProfileMapper).insert(any(UUID.class), eq("새이름"));
		verify(userSettingsMapper).insertDefaults(any(UUID.class));
		verify(oauthIdentityMapper).insert(any(UUID.class), eq((short) 2), eq("kakao-new"), eq("new@example.com"), eq("새이름"));
	}
}
