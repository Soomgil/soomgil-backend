package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SwipeReactionResponse(
	@Valid
	@NotNull
	PlaceRef place,
	@NotNull
	SwipeReaction reaction,
	@NotNull
	Boolean savedPlaceEligible,
	OffsetDateTime updatedAt
) {
}
