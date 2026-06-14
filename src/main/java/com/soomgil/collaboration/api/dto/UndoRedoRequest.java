package com.soomgil.collaboration.api.dto;

import jakarta.validation.constraints.NotNull;

public record UndoRedoRequest(
	@NotNull
	Long baseVersion,
	Long commandEventId
) {
}
