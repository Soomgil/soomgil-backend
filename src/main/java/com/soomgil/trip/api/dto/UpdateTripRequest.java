package com.soomgil.trip.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTripRequest(
	@Size(min = 1, max = 160)
	String title,
	@Size(max = 160)
	String displayDestination,
	List<String> legalRegionCodes,
	TripStatus status
) {
}
