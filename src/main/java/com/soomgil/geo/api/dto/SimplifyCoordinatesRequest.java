package com.soomgil.geo.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SimplifyCoordinatesRequest(
	@NotEmpty
	List<@Valid LngLat> coordinates,
	Integer maxPoints
) {
}
