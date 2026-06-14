package com.soomgil.collaboration.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CollaborationActionResponse(
	@NotNull
	UUID tripId,
	@NotNull
	Long itineraryVersion,
	Long commandEventId,
	@NotNull
	Boolean undoAvailable,
	@NotNull
	Boolean redoAvailable
) {
}
