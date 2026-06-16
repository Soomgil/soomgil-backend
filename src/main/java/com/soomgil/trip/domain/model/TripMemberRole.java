package com.soomgil.trip.domain.model;

/**
 * trip_members 테이블에 저장되는 멤버십 role.
 *
 * <p>MVP에서는 소유자도 {@code MEMBER}로 저장한다. OWNER 권한은
 * {@link Trip#ownerUserId()}와 요청 사용자 ID 비교로 파생한다.
 */
public enum TripMemberRole {
	MEMBER
}
