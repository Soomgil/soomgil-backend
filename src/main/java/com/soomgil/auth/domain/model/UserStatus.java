package com.soomgil.auth.domain.model;

/**
 * auth.users.status 값.
 *
 * <p>계정 수명 주기 상태를 나타낸다.
 */
public enum UserStatus {
	ACTIVE,
	PENDING,
	/** OAuth 신규 가입 직후 상태. 약관 동의·닉네임 확인 전까지 ACTIVE로 전환 안 됨. */
	PENDING_ONBOARDING,
	SUSPENDED,
	PENDING_DELETION,
	DELETED
}
