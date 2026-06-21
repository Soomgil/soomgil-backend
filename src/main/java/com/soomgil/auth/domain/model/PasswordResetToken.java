package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰.
 *
 * @param id 토큰 식별자
 * @param userId 대상 사용자 식별자
 * @param tokenHash SHA-256 hash
 * @param expiresAt 만료 시각
 * @param usedAt 사용 완료 시각 (nullable)
 */
public record PasswordResetToken(
	UUID id,
	UUID userId,
	String tokenHash,
	Instant expiresAt,
	Instant usedAt
) {

	public boolean isUsable() {
		return usedAt == null && expiresAt.isAfter(Instant.now());
	}
}
