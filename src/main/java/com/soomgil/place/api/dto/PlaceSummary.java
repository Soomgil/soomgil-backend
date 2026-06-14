package com.soomgil.place.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;

public record PlaceSummary(
	@NotNull
	PlaceProvider provider,
	@NotBlank
	@Size(max = 120)
	String externalPlaceId,
	@NotBlank
	String name,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String category,
	@NotNull
	PlaceSourceStatus sourceStatus
) {
}
