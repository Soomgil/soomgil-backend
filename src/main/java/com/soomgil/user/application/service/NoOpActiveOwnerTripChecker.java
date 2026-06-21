package com.soomgil.user.application.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * trip 모듈(김지훈) 구현 전까지 사용하는 {@link ActiveOwnerTripChecker} 기본 구현체.
 *
 * <p>항상 {@code false}를 반환하여, 활성 OWNER 여행방 검사가 없는 것과 동일하게 동작한다.
 * trip 모듈이 준비되면 본 bean을 대체하는 구현체를 추가하고 {@code @ConditionalOnMissingBean}
 * 또는 직접 교체로 전환한다.
 *
 * <p>TODO(trip): trip 모듈 연동 후 실제 구현체로 교체.
 */
@Component
public class NoOpActiveOwnerTripChecker implements ActiveOwnerTripChecker {

	@Override
	public boolean hasActiveOwnerTrip(UUID userId) {
		return false;
	}
}
