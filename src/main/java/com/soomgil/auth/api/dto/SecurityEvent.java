package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SecurityEvent(
	@NotNull
	Long id,
	@NotBlank
	String eventType,
	@NotNull
	Boolean success,
	String failureReason,
	@NotNull
	OffsetDateTime createdAt
) {
}
