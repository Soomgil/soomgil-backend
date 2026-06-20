package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.application.port.RouteCoordinate;
import com.soomgil.itinerary.domain.model.RouteMode;
import java.util.List;
import java.util.UUID;

/**
 * Mapbox map matching으로 route segment를 생성하는 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param originItineraryItemId 출발 일정 item ID
 * @param destinationItineraryItemId 도착 일정 item ID
 * @param mode 이동 mode
 * @param coordinates 원본 경로 좌표
 * @param radiuses 좌표별 탐색 반경
 * @param tidy Mapbox tidy 옵션
 */
public record MapMatchRouteCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID originItineraryItemId,
	UUID destinationItineraryItemId,
	RouteMode mode,
	List<RouteCoordinate> coordinates,
	List<Double> radiuses,
	Boolean tidy
) implements Command<MapMatchRouteResult> {
}
