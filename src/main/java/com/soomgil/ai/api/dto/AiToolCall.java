package com.soomgil.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AiToolCall(
	@NotNull
	UUID id,
	@NotBlank
	String toolName,
	@NotNull
	AiToolExecutionPolicy executionPolicy,
	@NotNull
	AiToolCallStatus status,
	Long versionBefore,
	Long versionAfter,
	Boolean undoRedoAvailable,
	String errorCode
) {
}
