package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.Optional;
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
	 * 여행방의 현재 itinerary version을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 여행방이 존재하면 현재 version
	 */
	OptionalLong findItineraryVersion(UUID tripId);

	/**
	 * 일정 day를 저장한다.
	 *
	 * @param day 저장할 day
	 */
	void insertDay(ItineraryDayCreate day);

	/**
	 * 여행방의 일차 미정 day를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 존재하면 일차 미정 day
	 */
	Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId);

	/**
	 * 일정 item을 저장한다.
	 *
	 * @param item 저장할 item
	 */
	void insertItem(ItineraryItemCreate item);

	/**
	 * 지도 도형을 저장한다.
	 *
	 * @param drawing 저장할 도형
	 */
	void insertMapDrawing(MapDrawingCreate drawing);

	/**
	 * day가 같은 trip에 존재하는지 확인한다.
	 *
	 * @param tripId 여행방 ID
	 * @param dayId 일정 day ID
	 * @return 존재 여부
	 */
	boolean existsDay(UUID tripId, UUID dayId);

	/**
	 * 여행방의 전체 itinerary day 수를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 전체 day 수
	 */
	long countDays(UUID tripId);

	/**
	 * item이 같은 trip에 존재하고 삭제되지 않았는지 확인한다.
	 *
	 * @param tripId 여행방 ID
	 * @param itemId 일정 item ID
	 * @return 존재 여부
	 */
	boolean existsItem(UUID tripId, UUID itemId);

	/**
	 * 여행방의 삭제되지 않은 active item 수를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return active item 수
	 */
	long countActiveItems(UUID tripId);

	/**
	 * 일정 day sort order를 갱신한다.
	 *
	 * @param update 갱신할 day 순서
	 */
	void updateDayOrder(ItineraryDayOrderUpdate update);

	/**
	 * 일정 item day 소속과 sort order를 갱신한다.
	 *
	 * @param update 갱신할 item 순서
	 */
	void updateItemOrder(ItineraryItemOrderUpdate update);
}
