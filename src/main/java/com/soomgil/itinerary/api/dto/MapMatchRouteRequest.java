package com.soomgil.itinerary.api.dto;

import com.soomgil.geo.api.dto.LngLat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

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
	@Size(min = 2, max = 100)
	List<LngLat> coordinates,
	List<Double> radiuses,
	Boolean tidy
) {
}
