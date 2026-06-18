package com.soomgil.auth.domain.model;

/**
 * auth.users.status 값.
 *
 * <p>계정 수명 주기 상태를 나타낸다.
 */
public enum UserStatus {
	ACTIVE,
	PENDING,
	SUSPENDED,
	PENDING_DELETION,
	DELETED
}
