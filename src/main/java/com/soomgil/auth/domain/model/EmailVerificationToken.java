package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 이메일 인증 토큰.
 *
 * @param id 토큰 식별자
 * @param emailAddressId 대상 이메일 주소 식별자
 * @param tokenHash SHA-256 hash (raw token은 저장하지 않음)
 * @param expiresAt 만료 시각
 * @param usedAt 사용 완료 시각 (nullable)
 */
public record EmailVerificationToken(
	UUID id,
	UUID emailAddressId,
	String tokenHash,
	Instant expiresAt,
	Instant usedAt
) {

	public boolean isUsable() {
		return usedAt == null && expiresAt.isAfter(Instant.now());
	}
}
