package com.soomgil.trip.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripSummary(
	@NotNull
	UUID id,
	@NotBlank
	String title,
	String displayDestination,
	@NotNull
	TripStatus status,
	@NotNull
	TripAccessRole myRole,
	@NotNull
	Long itineraryVersion,
	@NotNull
	OffsetDateTime createdAt
) {
}
