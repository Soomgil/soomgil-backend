package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * auth.user_sessions 테이블에 대응하는 refresh session model.
 *
 * <p>refresh token rotation과 재사용 감지에 사용된다. token hash만 저장하고 raw token은 저장하지 않는다.
 *
 * @param id session 식별자
 * @param userId 소속 사용자
 * @param refreshTokenHash refresh token의 SHA-256 hash
 * @param refreshTokenFamilyId rotation family 식별자
 * @param expiresAt session 만료 시각
 * @param revokedAt session revoke 시각
 * @param createdAt 생성 시각
 */
public record UserSession(
	UUID id,
	UUID userId,
	String refreshTokenHash,
	UUID refreshTokenFamilyId,
	Instant expiresAt,
	Instant revokedAt,
	Instant createdAt
) {

	/**
	 * session이 아직 유효한지 확인한다.
	 *
	 * @return 만료 전이고 revoke되지 않았으면 true
	 */
	public boolean isActive() {
		return revokedAt == null && expiresAt.isAfter(Instant.now());
	}
}
