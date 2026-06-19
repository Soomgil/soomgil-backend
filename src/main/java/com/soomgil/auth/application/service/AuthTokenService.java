package com.soomgil.auth.application.service;

import com.soomgil.global.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * access token과 refresh token을 발급한다.
 *
 * <p>access token은 HS256 JWT로 발급하고, refresh token은 opaque random token으로 발급한다.
 * refresh token은 DB에 hash만 저장하므로, {@link #hashRefreshToken(String)}으로 hash를 계산한다.
 */
@Component
public class AuthTokenService {

	/** refresh token 수명 (초). 7일. */
	public static final long REFRESH_TOKEN_TTL_SECONDS = 604_800;

	private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
	}

	/**
	 * JWT access token을 발급한다.
	 *
	 * @param userId 사용자 식별자 (sub claim)
	 * @param email 이메일 (email claim, nullable)
	 * @return JWT token 문자열
	 */
	public String mintAccessToken(UUID userId, String email) {
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer())
			.subject(userId.toString())
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(jwtProperties.accessTokenTtlSeconds()));
		if (email != null) {
			claims.claim("email", email);
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

	/**
	 * 새로운 opaque refresh token을 발급한다.
	 *
	 * @return raw token과 hash, 만료 시각을 담은 결과
	 */
	public IssuedRefreshToken mintRefreshToken() {
		byte[] randomBytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
		secureRandom.nextBytes(randomBytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
		String hash = hashRefreshToken(raw);
		Instant expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS);
		return new IssuedRefreshToken(raw, hash, expiresAt);
	}

	/**
	 * raw refresh token에서 hash를 계산한다. DB 조회용.
	 *
	 * @param rawToken raw refresh token
	 * @return SHA-256 hash (base64url)
	 */
	public String hashRefreshToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm not available", exception);
		}
	}

	/**
	 * 발급된 refresh token의 raw 값, hash, 만료 시각.
	 *
	 * @param raw 클라이언트에 반환할 raw token
	 * @param hash DB에 저장할 SHA-256 hash
	 * @param expiresAt 만료 시각
	 */
	public record IssuedRefreshToken(String raw, String hash, Instant expiresAt) {
	}
}
