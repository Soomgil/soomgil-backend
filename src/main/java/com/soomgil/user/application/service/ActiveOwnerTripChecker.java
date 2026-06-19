package com.soomgil.user.application.service;

import java.util.UUID;

/**
 * 사용자가 활성 여행방의 OWNER인지 확인하는 port.
 *
 * <p>계정 삭제({@code DELETE /me})는 MVP에서 OWNER 이관을 지원하지 않으므로,
 * 활성 OWNER 여행방이 있으면 삭제 예약을 차단한다({@code api_spec.md} 4.1.3 절).
 *
 * <p>trip 모듈(김지훈)이 구현되기 전까지는 {@code false}를 반환하는 기본 구현체
 * ({@code NoOpActiveOwnerTripChecker})를 사용한다. trip 모듈 완성 후 실제 구현체로 교체한다.
 */
public interface ActiveOwnerTripChecker {

	/**
	 * 사용자가 활성 여행방의 OWNER인지 확인한다.
	 *
	 * @param userId 사용자 식별자
	 * @return 활성 OWNER 여행방이 하나라도 있으면 {@code true}
	 */
	boolean hasActiveOwnerTrip(UUID userId);
}
