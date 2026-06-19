package com.soomgil.auth.application.command;

import java.util.UUID;

/**
 * 회원가입, 로그인, refresh 공통 반환 결과.
 *
 * <p>access token, refresh token, 최소 사용자 정보를 담는다.
 *
 * @param accessToken JWT access token
 * @param refreshToken opaque refresh token (raw)
 * @param userId 사용자 식별자
 * @param email 대표 이메일 (nullable)
 * @param displayName 표시 이름
 */
public record AuthTokenResult(
	String accessToken,
	String refreshToken,
	UUID userId,
	String email,
	String displayName
) {
}
