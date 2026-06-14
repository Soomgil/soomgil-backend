package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateItineraryDayRequest(
	@NotNull
	Long baseVersion,
	@NotNull
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	Integer sortOrder
) {
}
