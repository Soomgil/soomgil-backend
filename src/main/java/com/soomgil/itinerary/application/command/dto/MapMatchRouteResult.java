package com.soomgil.itinerary.application.command.dto;

import java.util.List;
import java.util.Map;

/**
 * Map matching route 생성 결과.
 *
 * @param mutation 생성된 route segment mutation 결과
 * @param matchRequestId route matching 요청 이력 ID
 * @param tracepoints provider tracepoint 목록
 * @param matchingsMetadata provider matching metadata
 */
public record MapMatchRouteResult(
	ItineraryMutationResult mutation,
	Long matchRequestId,
	List<Map<String, Object>> tracepoints,
	Map<String, Object> matchingsMetadata
) {
}
