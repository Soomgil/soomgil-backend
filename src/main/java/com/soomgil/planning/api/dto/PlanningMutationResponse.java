package com.soomgil.planning.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PlanningMutationResponse(
	@NotNull
	UUID tripId,
	@NotNull
	Long itineraryVersion,
	Long commandEventId,
	@NotNull
	Boolean undoAvailable,
	@NotNull
	Boolean redoAvailable,
	@Valid
	Note note,
	@Valid
	Checklist checklist,
	@Valid
	ChecklistItem item,
	@Valid
	ChecklistMemberStatus memberStatus
) {
}
