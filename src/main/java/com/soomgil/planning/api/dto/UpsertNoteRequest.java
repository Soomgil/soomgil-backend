package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpsertNoteRequest(
	@NotNull
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	@NotBlank
	String content
) {
}
