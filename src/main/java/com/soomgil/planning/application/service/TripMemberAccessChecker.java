package com.soomgil.planning.application.service;

import java.util.UUID;

/**
 * planning mutation/query 전 사용자가 해당 여행방의 active member인지 검증한다.
 *
 * <p>구현체는 trip 모듈의 접근 guard를 통해 active member 여부를 검증한다.
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
