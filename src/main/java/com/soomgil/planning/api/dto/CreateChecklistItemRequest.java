package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChecklistItemRequest(
	@NotNull
	Long baseVersion,
	@NotBlank
	String content,
	Integer sortOrder
) {
}
