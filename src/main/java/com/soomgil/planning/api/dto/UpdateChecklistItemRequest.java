package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateChecklistItemRequest(
	@NotNull
	Long baseVersion,
	String content,
	Integer sortOrder
) {
}
