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
 * @param onboarded 온보딩(닉네임 확인 + 필수 약관 동의) 완료 여부.
 *                  OAuth 신규 가입자는 false로 내려가고, 프론트가 /register?oauth=1로 보낸다.
 */
public record AuthTokenResult(
	String accessToken,
	String refreshToken,
	UUID userId,
	String email,
	String displayName,
	boolean onboarded
) {
}
