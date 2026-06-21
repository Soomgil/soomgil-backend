package com.soomgil.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * auth.users 테이블에 대응하는 계정 root aggregate (최소 표현).
 *
 * <p>계정 식별자, 상태, 수명 주기 시간을 가진다. 이메일/비밀번호/프로필은 별도 model로 분리한다.
 *
 * @param id 계정 고유 식별자
 * @param status 계정 상태
 * @param lastLoginAt 마지막 로그인 시각
 * @param createdAt 생성 시각
 */
public record AuthUser(
	UUID id,
	UserStatus status,
	Instant lastLoginAt,
	Instant createdAt
) {

	/**
	 * 계정이 로그인 가능한 상태인지 확인한다.
	 *
	 * @return ACTIVE 상태면 true
	 */
	public boolean canLogin() {
		return status == UserStatus.ACTIVE || status == UserStatus.PENDING_ONBOARDING;
	}
}
