package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripMember;
import java.util.List;

/**
 * 여행방 쓰기 persistence 계약.
 *
 * <p>구현체는 여행방 생성과 최초 멤버십 저장을 같은 transaction 안에서 수행해야 한다.
 */
public interface TripCommandRepository {

	/**
	 * 생성된 여행방과 생성자의 최초 멤버십을 저장한다.
	 *
	 * @param trip 저장할 여행방
	 * @param initialMember 생성자 active member
	 * @param legalRegionCodes 선택적 법정동 코드 목록
	 */
	void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes);
}
