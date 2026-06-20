package com.soomgil.trip.domain.model;

/**
 * 여행방의 생명주기 상태.
 *
 * <p>{@code ACTIVE} 여행방만 일반 사용자 기능에서 접근 가능하다.
 * {@code DELETED}는 soft delete 상태이며 접근 권한이 있던 멤버도 사용할 수 없다.
 */
public enum TripStatus {
	ACTIVE,
	ARCHIVED,
	DELETED
}
