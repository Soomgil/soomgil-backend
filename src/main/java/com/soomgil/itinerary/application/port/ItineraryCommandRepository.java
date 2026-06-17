package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * 일정 day/item 쓰기 persistence 계약.
 *
 * <p>version 증가는 {@code trip.trips.itinerary_version}을 기준으로 처리한다.
 */
public interface ItineraryCommandRepository {

	/**
	 * baseVersion이 현재 version과 일치할 때만 여행방 itinerary version을 증가시킨다.
	 *
	 * @param tripId 여행방 ID
	 * @param baseVersion 요청자가 본 이전 version
	 * @param updatedAt 갱신 시각
	 * @return 증가된 version, 불일치하면 empty
	 */
	OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt);

	/**
	 * 일정 day를 저장한다.
	 *
	 * @param day 저장할 day
	 */
	void insertDay(ItineraryDayCreate day);

	/**
	 * 일정 item을 저장한다.
	 *
	 * @param item 저장할 item
	 */
	void insertItem(ItineraryItemCreate item);

	/**
	 * day가 같은 trip에 존재하는지 확인한다.
	 *
	 * @param tripId 여행방 ID
	 * @param dayId 일정 day ID
	 * @return 존재 여부
	 */
	boolean existsDay(UUID tripId, UUID dayId);
}
