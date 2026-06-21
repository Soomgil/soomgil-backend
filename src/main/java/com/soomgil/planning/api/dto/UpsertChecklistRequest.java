package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpsertChecklistRequest(
	@NotNull
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String title
) {
}
