package com.soomgil.itinerary.application.query.dto;

import com.soomgil.itinerary.application.command.dto.MapDrawingView;
import com.soomgil.itinerary.application.command.dto.RouteSegmentView;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 itinerary 전체 조회 view.
 *
 * @param tripId 여행방 ID
 * @param itineraryVersion 현재 itinerary version
 * @param days item을 포함한 day 목록
 * @param routes route segment 목록
 * @param mapDrawings 지도 도형 목록
 */
public record ItineraryView(
	UUID tripId,
	Long itineraryVersion,
	List<ItineraryDayDetailView> days,
	List<RouteSegmentView> routes,
	List<MapDrawingView> mapDrawings
) {
}
