package com.soomgil.trip.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateTripRequest(
	@NotBlank
	@Size(min = 1, max = 160)
	String title,
	@Size(max = 160)
	String displayDestination,
	List<String> legalRegionCodes,
	LocalDate startDate,
	LocalDate endDate
) {
}
