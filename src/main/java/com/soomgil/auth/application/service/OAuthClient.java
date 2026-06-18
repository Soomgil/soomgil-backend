package com.soomgil.auth.application.service;

import com.soomgil.auth.api.dto.OAuthProviderCode;
import com.soomgil.auth.domain.model.AuthException;
import com.soomgil.global.error.ErrorCode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * OAuth 제공자(Kakao, Google)와 통신한다.
 *
 * <p>인증 URL 생성, authorization code 교환, 사용자 정보 조회를 담당한다.
 */
@Component
public class OAuthClient {

	private static final Logger log = LoggerFactory.getLogger(OAuthClient.class);

	private final RestClient restClient = RestClient.builder().build();
	private final OAuthProperties properties;

	public OAuthClient(OAuthProperties properties) {
		this.properties = properties;
	}

	/**
	 * OAuth 인증 URL을 생성한다.
	 *
	 * @param provider 제공자 코드
	 * @param redirectUri 리다이렉트 URI (null이면 설정 기본값 사용)
	 * @param state CSRF 방지용 state (null이면 자동 생성)
	 * @return 인증 URL
	 */
	public String getAuthorizationUrl(OAuthProviderCode provider, String redirectUri, String state) {
		OAuthProperties.ProviderConfig config = properties.get(provider.name());
		if (!config.isConfigured()) {
			throw new AuthException(ErrorCode.OAUTH_NOT_CONFIGURED);
		}

		String uri = redirectUri != null ? redirectUri : config.redirectUri();
		String st = state != null ? state : UUID.randomUUID().toString();

		return switch (provider) {
			case KAKAO -> "https://kauth.kakao.com/oauth/authorize"
				+ "?client_id=" + enc(config.clientId())
				+ "&redirect_uri=" + enc(uri)
				+ "&response_type=code"
				+ "&state=" + enc(st);
			case GOOGLE -> "https://accounts.google.com/o/oauth2/v2/auth"
				+ "?client_id=" + enc(config.clientId())
				+ "&redirect_uri=" + enc(uri)
				+ "&response_type=code"
				+ "&scope=" + enc("openid email profile")
				+ "&state=" + enc(st);
		};
	}

	/**
	 * authorization code를 access token으로 교환하고 사용자 정보를 조회한다.
	 *
	 * @param provider 제공자 코드
	 * @param code authorization code
	 * @param redirectUri 리다이렉트 URI
	 * @return 제공자 사용자 정보
	 */
	@SuppressWarnings("unchecked")
	public ProviderUserInfo getUserInfo(OAuthProviderCode provider, String code, String redirectUri) {
		OAuthProperties.ProviderConfig config = properties.get(provider.name());
		if (!config.isConfigured()) {
			throw new AuthException(ErrorCode.OAUTH_NOT_CONFIGURED);
		}

		try {
			return switch (provider) {
				case KAKAO -> getKakaoUserInfo(config, code, redirectUri);
				case GOOGLE -> getGoogleUserInfo(config, code, redirectUri);
			};
		} catch (HttpClientErrorException e) {
			log.error("OAuth provider error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new AuthException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	private ProviderUserInfo getKakaoUserInfo(OAuthProperties.ProviderConfig config, String code, String redirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", config.clientId());
		form.add("client_secret", config.clientSecret());
		form.add("redirect_uri", redirectUri);
		form.add("code", code);

		Map<String, Object> tokenResp = restClient.post()
			.uri("https://kauth.kakao.com/oauth/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(Map.class);

		if (tokenResp == null || !tokenResp.containsKey("access_token")) {
			throw new AuthException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}

		String accessToken = (String) tokenResp.get("access_token");

		Map<String, Object> userInfo = restClient.get()
			.uri("https://kapi.kakao.com/v2/user/me")
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(Map.class);

		if (userInfo == null) {
			throw new AuthException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}

		String providerSubject = String.valueOf(userInfo.get("id"));
		Map<String, Object> account = (Map<String, Object>) userInfo.get("kakao_account");
		Map<String, Object> profile = account != null ? (Map<String, Object>) account.get("profile") : null;
		String email = account != null ? (String) account.get("email") : null;
		String displayName = profile != null ? (String) profile.get("nickname") : null;

		return new ProviderUserInfo(providerSubject, email, displayName);
	}

	@SuppressWarnings("unchecked")
	private ProviderUserInfo getGoogleUserInfo(OAuthProperties.ProviderConfig config, String code, String redirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", config.clientId());
		form.add("client_secret", config.clientSecret());
		form.add("redirect_uri", redirectUri);
		form.add("code", code);

		Map<String, Object> tokenResp = restClient.post()
			.uri("https://oauth2.googleapis.com/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(Map.class);

		if (tokenResp == null || !tokenResp.containsKey("access_token")) {
			throw new AuthException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}

		String accessToken = (String) tokenResp.get("access_token");

		Map<String, Object> userInfo = restClient.get()
			.uri("https://www.googleapis.com/oauth2/v2/userinfo")
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.body(Map.class);

		if (userInfo == null) {
			throw new AuthException(ErrorCode.OAUTH_PROVIDER_ERROR);
		}

		return new ProviderUserInfo(
			(String) userInfo.get("id"),
			(String) userInfo.get("email"),
			(String) userInfo.get("name")
		);
	}

	private String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * OAuth 제공자로부터 받은 사용자 정보.
	 *
	 * @param providerSubject 제공자 내 사용자 식별자
	 * @param email 이메일 (nullable)
	 * @param displayName 표시 이름 (nullable)
	 */
	public record ProviderUserInfo(String providerSubject, String email, String displayName) {
	}
}
