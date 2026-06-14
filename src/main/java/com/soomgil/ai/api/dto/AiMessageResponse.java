package com.soomgil.ai.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AiMessageResponse(
	@Valid
	@NotNull
	AiChatMessage message,
	@Valid
	List<AiToolCall> toolCalls,
	Long itineraryVersion,
	Boolean undoAvailable,
	Boolean redoAvailable
) {
}
