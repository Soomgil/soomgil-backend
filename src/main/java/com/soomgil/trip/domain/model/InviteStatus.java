package com.soomgil.trip.domain.model;

/**
 * 여행방 초대의 처리 상태.
 *
 * <p>{@code PENDING} 초대만 수락 가능하다. 만료 판정은 status와 expiresAt을 함께 사용한다.
 */
public enum InviteStatus {
	PENDING,
	ACCEPTED,
	REVOKED,
	EXPIRED
}
