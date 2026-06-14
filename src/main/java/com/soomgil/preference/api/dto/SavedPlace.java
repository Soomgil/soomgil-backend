package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedPlace(
	@NotNull
	UUID id,
	@Valid
	@NotNull
	PlaceSummary place,
	@NotNull
	OffsetDateTime createdAt
) {
}
