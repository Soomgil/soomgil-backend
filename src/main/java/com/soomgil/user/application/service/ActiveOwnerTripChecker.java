package com.soomgil.user.application.service;

import java.util.UUID;

/**
 * 사용자가 활성 여행방의 OWNER인지 확인하는 port.
 *
 * <p>계정 삭제({@code DELETE /me})는 MVP에서 OWNER 이관을 지원하지 않으므로,
 * 활성 OWNER 여행방이 있으면 삭제 예약을 차단한다({@code api_spec.md} 4.1.3 절).
 *
	* <p>구현체는 trip 모듈의 조회 계약을 사용하며 user 모듈이 trip persistence를 직접 읽지 않는다.
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
