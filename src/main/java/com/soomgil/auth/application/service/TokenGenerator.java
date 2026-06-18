package com.soomgil.auth.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 / 비밀번호 재설정용 opaque 토큰을 발급한다.
 *
 * <p>{@link AuthTokenService#hashRefreshToken(String)}와 동일한 SHA-256 hash 알고리즘을 사용한다.
 * raw 토큰은 클라이언트에게만 전달하고, DB에는 hash만 저장한다.
 */
@Component
public class TokenGenerator {

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 32바이트 랜덤 토큰을 발급하고 hash를 계산한다.
	 *
	 * @return raw 토큰과 SHA-256 hash
	 */
	public GeneratedToken generate() {
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
		String hash = hash(raw);
		return new GeneratedToken(raw, hash);
	}

	/**
	 * raw 토큰에서 SHA-256 hash를 계산한다. DB 조회용.
	 *
	 * @param rawToken raw 토큰
	 * @return base64url encoded SHA-256 hash
	 */
	public String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm not available", exception);
		}
	}

	/**
	 * 발급된 토큰의 raw 값과 hash.
	 *
	 * @param raw 클라이언트에 반환할 raw 토큰
	 * @param hash DB에 저장할 hash
	 */
	public record GeneratedToken(String raw, String hash) {
	}
}
