package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** geometry 없이 mode를 지정하면 기존 경로 양 끝점 사이를 재계산한다. 실패 시 기존 구간은 유지된다. */
public record UpdateRouteRequest(
	@NotNull
	Long baseVersion,
	RouteMode mode,
	Map<String, Object> geometry,
	Double distanceMeters,
	Double durationSeconds,
	Double confidence
) {
}
