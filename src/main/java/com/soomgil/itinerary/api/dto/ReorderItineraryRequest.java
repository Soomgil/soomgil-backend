package com.soomgil.itinerary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderItineraryRequest(
	@NotNull
	Long baseVersion,
	@Valid
	@NotNull
	List<ItineraryDayOrder> days
) {
}
