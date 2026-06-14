package com.soomgil.itinerary.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryDay(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotNull
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	@NotNull
	Integer sortOrder,
	@Valid
	@NotNull
	List<ItineraryItem> items
) {
}
