package com.soomgil.itinerary.application.port;

import java.util.List;
import java.util.Map;

/**
 * 외부 map matching provider 성공 결과.
 *
 * @param geometry 확정 GeoJSON geometry
 * @param tracepoints provider tracepoint 목록
 * @param matchingsMetadata provider matching metadata
 * @param distanceMeters 거리(m)
 * @param durationSeconds 소요 시간(초)
 * @param confidence matching 신뢰도
 */
public record MapMatchClientResult(
	Map<String, Object> geometry,
	List<Map<String, Object>> tracepoints,
	Map<String, Object> matchingsMetadata,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) {
}
