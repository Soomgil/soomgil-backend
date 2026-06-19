package com.soomgil.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.global.security.JwtProperties;
import com.soomgil.global.security.JwtConfiguration;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * {@link AuthTokenService} 단위 테스트.
 *
 * <p>access token 발급과 refresh token 발급/hash 계산을 검증한다.
 * 실제 HS256 key와 JwtEncoder/JwtDecoder 빈을 사용한다.
 */
class AuthTokenServiceTest {

	private static final String TEST_SECRET = "Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz";

	private final JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, 900, "soomgil-test");
	private final JwtConfiguration jwtConfiguration = new JwtConfiguration();

	private final javax.crypto.SecretKey secretKey = jwtConfiguration.jwtSecretKey(jwtProperties);
	private final org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder = jwtConfiguration.jwtEncoder(secretKey);
	private final JwtDecoder jwtDecoder = jwtConfiguration.jwtDecoder(secretKey, jwtProperties);

	private final AuthTokenService authTokenService = new AuthTokenService(jwtEncoder, jwtProperties);

	@Test
	@DisplayName("access token은 sub claim에 userId를 담는다")
	void accessTokenContainsUserIdAsSubject() {
		UUID userId = UUID.randomUUID();

		String token = authTokenService.mintAccessToken(userId, "user@example.com");

		Jwt jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("user@example.com");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("soomgil-test");
	}

	@Test
	@DisplayName("email이 null이면 access token에 email claim이 없다")
	void accessTokenWithoutEmailClaim() {
		UUID userId = UUID.randomUUID();

		String token = authTokenService.mintAccessToken(userId, null);

		Jwt jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		assertThat(jwt.getClaims().containsKey("email")).isFalse();
	}

	@Test
	@DisplayName("refresh token은 매번 다른 raw 값을 생성한다")
	void mintRefreshTokenProducesUniqueTokens() {
		AuthTokenService.IssuedRefreshToken first = authTokenService.mintRefreshToken();
		AuthTokenService.IssuedRefreshToken second = authTokenService.mintRefreshToken();

		assertThat(first.raw()).isNotEqualTo(second.raw());
		assertThat(first.hash()).isNotEqualTo(second.hash());
	}

	@Test
	@DisplayName("hashRefreshToken은 같은 입력에 대해 같은 hash를 반환한다")
	void hashRefreshTokenIsDeterministic() {
		String raw = "some-refresh-token-value";

		String hash1 = authTokenService.hashRefreshToken(raw);
		String hash2 = authTokenService.hashRefreshToken(raw);

		assertThat(hash1).isEqualTo(hash2);
	}

	@Test
	@DisplayName("refresh token의 만료 시각은 발급 시점보다 7일 이후다")
	void refreshTokenExpiresAfter7Days() {
		AuthTokenService.IssuedRefreshToken token = authTokenService.mintRefreshToken();

		Duration ttl = Duration.between(java.time.Instant.now(), token.expiresAt());

		assertThat(ttl.toSeconds()).isGreaterThan(604_700L);
		assertThat(ttl.toSeconds()).isLessThan(604_900L);
	}

	@Test
	@DisplayName("mintRefreshToken의 hash는 hashRefreshToken과 일치한다")
	void mintedRefreshTokenHashMatchesManualHash() {
		AuthTokenService.IssuedRefreshToken token = authTokenService.mintRefreshToken();

		String manualHash = authTokenService.hashRefreshToken(token.raw());

		assertThat(token.hash()).isEqualTo(manualHash);
	}
}
