package com.soomgil.planning.application.service;

import java.util.UUID;

/**
 * planning mutation/query 전 사용자가 해당 여행방의 active member인지 검증한다.
 *
 * <p>여행방(trip) 모듈은 아직 구현되지 않았으므로, 기본 구현체
 * {@link NoOpTripMemberAccessChecker}는 어떤 검증도 수행하지 않는다.
 * trip 모듈 완성 후 실제 구현체로 교체한다.
 */
public interface TripMemberAccessChecker {

	/**
	 * 사용자가 여행방의 active member인지 검증한다. 권한이 부족하면 예외를 던진다.
	 *
	 * @param tripId 여행방 식별자
	 * @param userId 요청자 식별자
	 */
	void requireMember(UUID tripId, UUID userId);
}
