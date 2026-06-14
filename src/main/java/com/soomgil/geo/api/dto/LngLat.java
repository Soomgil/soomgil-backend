package com.soomgil.geo.api.dto;

import jakarta.validation.constraints.NotNull;

public record LngLat(
	@NotNull
	Double lng,
	@NotNull
	Double lat
) {
}
