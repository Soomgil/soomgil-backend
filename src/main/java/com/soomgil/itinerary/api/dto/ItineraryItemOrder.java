package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ItineraryItemOrder(
	@NotNull
	UUID itemId,
	@NotNull
	Integer sortOrder
) {
}
