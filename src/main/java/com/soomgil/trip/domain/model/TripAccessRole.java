package com.soomgil.trip.domain.model;

/**
 * API와 다른 모듈에 노출하는 파생 접근 role.
 *
 * <p>{@code OWNER}는 trip_members.role에 저장하지 않고
 * 여행방의 {@code ownerUserId}와 요청 사용자 ID가 같을 때만 파생한다.
 */
public enum TripAccessRole {
	OWNER,
	MEMBER
}
