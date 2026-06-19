package com.soomgil.auth.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth 제공자(Kakao, Google) 클라이언트 설정.
 *
 * <p>환경변수로 client-id / client-secret을 주입한다. 값이 비어 있으면
 * 해당 제공자의 OAuth 엔드포인트는 {@code OAUTH_NOT_CONFIGURED (503)}을 반환한다.
 *
 * <pre>
 * soomgil.security.oauth:
 *   kakao:
 *     client-id: ${KAKAO_CLIENT_ID:}
 *     client-secret: ${KAKAO_CLIENT_SECRET:}
 *     redirect-uri: ${KAKAO_REDIRECT_URI:http://localhost:5173/auth/oauth/kakao/callback}
 *   google:
 *     client-id: ${GOOGLE_CLIENT_ID:}
 *     client-secret: ${GOOGLE_CLIENT_SECRET:}
 *     redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:5173/auth/oauth/google/callback}
 * </pre>
 */
@ConfigurationProperties(prefix = "soomgil.security.oauth")
public record OAuthProperties(
	ProviderConfig kakao,
	ProviderConfig google
) {

	/**
	 * 개별 OAuth 제공자 설정.
	 *
	 * @param clientId 클라이언트 ID
	 * @param clientSecret 클라이언트 시크릿
	 * @param redirectUri 리다이렉트 URI
	 */
	public record ProviderConfig(
		String clientId,
		String clientSecret,
		String redirectUri
	) {

		public boolean isConfigured() {
			return clientId != null && !clientId.isBlank()
				&& clientSecret != null && !clientSecret.isBlank();
		}
	}

	/**
	 * 제공자 코드로 설정을 조회한다.
	 *
	 * @param providerCode "KAKAO" 또는 "GOOGLE"
	 * @return 제공자 설정 (null이 아님)
	 */
	public ProviderConfig get(String providerCode) {
		return switch (providerCode.toUpperCase()) {
			case "KAKAO" -> kakao != null ? kakao : new ProviderConfig(null, null, null);
			case "GOOGLE" -> google != null ? google : new ProviderConfig(null, null, null);
			default -> throw new IllegalArgumentException("Unknown OAuth provider: " + providerCode);
		};
	}
}
