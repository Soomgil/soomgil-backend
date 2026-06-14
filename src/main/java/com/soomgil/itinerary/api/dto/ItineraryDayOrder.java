package com.soomgil.itinerary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ItineraryDayOrder(
	@NotNull
	UUID dayId,
	@NotNull
	Integer sortOrder,
	@Valid
	@NotNull
	List<ItineraryItemOrder> itemOrders
) {
}
