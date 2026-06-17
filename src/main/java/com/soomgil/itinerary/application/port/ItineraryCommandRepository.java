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
	 * day ID로 itinerary day를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param dayId day ID
	 * @return 존재하면 day
	 */
	Optional<ItineraryDayReadModel> findDay(UUID tripId, UUID dayId);

	/**
	 * 여행방의 일차 미정 day를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 존재하면 일차 미정 day
	 */
	Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId);

	/**
	 * itinerary day를 수정하고 수정 후 값을 반환한다.
	 *
	 * @param update 수정 모델
	 * @return 수정된 day, 없으면 empty
	 */
	Optional<ItineraryDayReadModel> updateDay(ItineraryDayUpdate update);

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
	 * route segment를 저장한다.
	 *
	 * @param route 저장할 route segment
	 */
	void insertRouteSegment(RouteSegmentCreate route);

	/**
	 * route map matching 요청 이력을 저장한다.
	 *
	 * @param request 요청 이력
	 * @return 저장된 요청 이력 ID
	 */
	Long insertRouteMatchRequest(RouteMatchRequestLog request);

	/**
	 * active route segment가 같은 trip에 존재하는지 확인한다.
	 *
	 * @param tripId 여행방 ID
	 * @param routeId route ID
	 * @return 존재 여부
	 */
	boolean existsActiveRouteSegment(UUID tripId, UUID routeId);

	/**
	 * route segment를 soft delete 처리한다.
	 *
	 * @param tripId 여행방 ID
	 * @param routeId route ID
	 * @param deletedByUserId 삭제 사용자 ID
	 * @param deletedAt 삭제 시각
	 * @return 삭제된 row가 있으면 true
	 */
	boolean softDeleteRouteSegment(UUID tripId, UUID routeId, UUID deletedByUserId, Instant deletedAt);

	/**
	 * active map drawing이 같은 trip에 존재하는지 확인한다.
	 *
	 * @param tripId 여행방 ID
	 * @param drawingId drawing ID
	 * @return 존재 여부
	 */
	boolean existsActiveMapDrawing(UUID tripId, UUID drawingId);

	/**
	 * map drawing을 soft delete 처리한다.
	 *
	 * @param tripId 여행방 ID
	 * @param drawingId drawing ID
	 * @param deletedByUserId 삭제 사용자 ID
	 * @param deletedAt 삭제 시각
	 * @return 삭제된 row가 있으면 true
	 */
	boolean softDeleteMapDrawing(UUID tripId, UUID drawingId, UUID deletedByUserId, Instant deletedAt);

	/**
	 * map drawing을 수정하고 수정 후 값을 반환한다.
	 *
	 * @param update 수정 모델
	 * @return 수정된 drawing, version 조건 불일치 또는 미존재 시 empty
	 */
	Optional<MapDrawingUpdateResult> updateMapDrawing(MapDrawingUpdate update);

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
	 * item에 연결된 active route ID 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param itemId item ID
	 * @return 연결 route ID 목록
	 */
	java.util.List<UUID> findActiveRouteIdsByItem(UUID tripId, UUID itemId);

	/**
	 * itinerary item을 soft delete 처리한다.
	 *
	 * @param tripId 여행방 ID
	 * @param itemId item ID
	 * @param deletedByUserId 삭제 사용자 ID
	 * @param deletedAt 삭제 시각
	 * @return 삭제된 row가 있으면 true
	 */
	boolean softDeleteItem(UUID tripId, UUID itemId, UUID deletedByUserId, Instant deletedAt);

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
