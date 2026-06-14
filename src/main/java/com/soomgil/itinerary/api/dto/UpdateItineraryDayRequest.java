package com.soomgil.itinerary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateItineraryDayRequest(
	@NotNull
	Long baseVersion,
	Integer dayNumber,
	LocalDate date,
	String title,
	Integer sortOrder
) {
}
