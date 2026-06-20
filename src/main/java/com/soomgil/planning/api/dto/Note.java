package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Note(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotNull
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	@NotBlank
	String content,
	OffsetDateTime deletedAt
) {
}
