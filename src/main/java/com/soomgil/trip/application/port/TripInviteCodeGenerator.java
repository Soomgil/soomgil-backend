package com.soomgil.trip.application.port;

/**
 * 여행방 초대 code를 생성하는 계약.
 *
 * <p>테스트는 고정 generator를 사용하고, 운영 구현은 충분히 예측하기 어려운 값을 생성한다.
 */
@FunctionalInterface
public interface TripInviteCodeGenerator {

	/**
	 * 새 초대 code를 생성한다.
	 *
	 * @return 사용자에게 노출 가능한 초대 code
	 */
	String generate();
}
