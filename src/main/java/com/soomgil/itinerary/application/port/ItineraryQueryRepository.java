package com.soomgil.itinerary.application.port;

import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * itinerary 읽기 persistence 계약.
 */
public interface ItineraryQueryRepository {

	/**
	 * 여행방의 현재 itinerary version을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 여행방이 존재하면 현재 version
	 */
	OptionalLong findItineraryVersion(UUID tripId);

	/**
	 * 여행방의 itinerary day 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return day 목록
	 */
	List<ItineraryDayReadModel> findDays(UUID tripId);

	/**
	 * 여행방의 active itinerary item 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return item 목록
	 */
	List<ItineraryItemReadModel> findItems(UUID tripId);

	/**
	 * 여행방의 active route segment 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return route 목록
	 */
	List<RouteSegmentReadModel> findRoutes(UUID tripId);

	/**
	 * 여행방의 active map drawing 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return drawing 목록
	 */
	List<MapDrawingReadModel> findMapDrawings(UUID tripId);
}
