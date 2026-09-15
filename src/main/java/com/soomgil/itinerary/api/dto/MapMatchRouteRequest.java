package com.soomgil.itinerary.api.dto;

import com.soomgil.geo.api.dto.LngLat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** 선택한 이동수단으로 2~25개의 경유점을 연결하는 Directions 요청. tidy는 호환성 필드로 사용하지 않는다. */
public record MapMatchRouteRequest(
	@NotNull
	Long baseVersion,
	@NotNull
	UUID originItineraryItemId,
	@NotNull
	UUID destinationItineraryItemId,
	@NotNull
	RouteMode mode,
	@Valid
	@NotNull
	@Size(min = 2, max = 25)
	List<LngLat> coordinates,
	List<Double> radiuses,
	Boolean tidy
) {
}
