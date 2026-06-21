package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChecklistItemRequest(
	@NotBlank
	String content,
	Integer sortOrder
) {
}
