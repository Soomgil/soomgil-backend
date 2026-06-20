package com.soomgil.planning.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record Checklist(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotNull
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String title,
	@Valid
	@NotNull
	List<ChecklistItem> items
) {
}
