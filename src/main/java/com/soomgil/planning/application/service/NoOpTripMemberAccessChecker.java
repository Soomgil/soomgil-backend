package com.soomgil.planning.application.service;

import java.util.UUID;

/**
 * {@link TripMemberAccessChecker}의 stub 구현.
 *
 * <p>여행방(trip) 모듈이 구현되기 전까지 사용된다. 실제 여행방 데이터를 검증하지 않고
 * 어떤 호출도 무시한다.
 *
 * <p>{@link com.soomgil.planning.application.config.PlanningModuleConfig}에서
 * {@code @ConditionalOnMissingBean}으로 등록되므로, trip 모듈에서
 * {@link TripMemberAccessChecker}를 구현한 bean을 등록하면 이 stub은 자동으로 비활성화된다.
 */
public class NoOpTripMemberAccessChecker implements TripMemberAccessChecker {

	@Override
	public void requireMember(UUID tripId, UUID userId) {
		// TODO: trip 모듈 완성 후 실제 구현체로 교체
		// 현재는 권한 검증을 생략한다. SecurityConfig의 authenticated()만 1차 방어.
	}
}
