package com.soomgil.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiChatSession(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotBlank
	String status,
	OffsetDateTime summaryUpdatedAt,
	OffsetDateTime createdAt
) {
}
