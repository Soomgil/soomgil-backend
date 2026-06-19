package com.soomgil.community.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 커뮤니티 게시글 공유 토큰을 발급/검증한다.
 *
 * <p>32바이트 난수를 Base64URL로 인코딩해 raw token을 만들고, SHA-256 hash를 DB에 저장한다.
 * raw token은 발급/rotate 응답에서 딱 한 번만 클라이언트에 반환한다.
 *
 * <p>refresh token 패턴과 동일한 구조. {@code ShareTokenService}는 커뮤니티 도메인에
 * 국한되므로 {@code AuthTokenService}와 분리했다.
 */
@Component
public class ShareTokenService {

	private static final int TOKEN_BYTE_LENGTH = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 새 raw token과 그 hash를 발급한다.
	 *
	 * @return raw + hash
	 */
	public IssuedShareToken issue() {
		byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
		secureRandom.nextBytes(randomBytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
		return new IssuedShareToken(raw, hash(raw));
	}

	/**
	 * raw token을 SHA-256 hash로 변환한다. DB 조회/저장용.
	 *
	 * @param rawToken raw token
	 * @return base64url-encoded SHA-256 hash
	 */
	public String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

	/**
	 * 발급된 공유 토큰.
	 *
	 * @param raw 클라이언트에 반환할 raw token (DB 저장 ❌)
	 * @param hash DB에 저장할 SHA-256 hash
	 */
	public record IssuedShareToken(String raw, String hash) {
	}
}
