package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * auth.user_password_credentials 테이블에 대응하는 비밀번호 자격 증명 model.
 *
 * <p>비밀번호는 hash 형태로만 저장된다. raw password는 저장하지 않는다.
 *
 * @param userId 소속 사용자 식별자
 * @param passwordHash bcrypt hash 문자열
 * @param failedLoginCount 연속 로그인 실패 횟수
 * @param lockedUntil 계정 잠금 만료 시각
 */
public record PasswordCredential(
	UUID userId,
	String passwordHash,
	int failedLoginCount,
	Instant lockedUntil
) {

	/**
	 * 계정이 현재 잠겨 있는지 확인한다.
	 *
	 * @return lockedUntil이 현재 시각 이후면 true
	 */
	public boolean isLocked() {
		return lockedUntil != null && lockedUntil.isAfter(Instant.now());
	}
}
