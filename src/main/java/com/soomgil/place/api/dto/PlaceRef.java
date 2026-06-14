package com.soomgil.place.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceRef(
	@NotNull
	PlaceProvider provider,
	@NotBlank
	@Size(max = 120)
	String externalPlaceId
) {
}
