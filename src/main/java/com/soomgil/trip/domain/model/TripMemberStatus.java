package com.soomgil.trip.domain.model;

/**
 * 여행방 멤버십의 현재 상태.
 *
 * <p>{@code ACTIVE} 멤버만 여행방에 접근할 수 있다. 나가거나 제거된 멤버의
 * 과거 row는 보존하더라도 접근 권한에는 사용하지 않는다.
 */
public enum TripMemberStatus {
	ACTIVE,
	LEFT,
	REMOVED
}
