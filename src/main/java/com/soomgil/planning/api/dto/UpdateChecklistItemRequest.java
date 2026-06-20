package com.soomgil.planning.api.dto;

public record UpdateChecklistItemRequest(
	String content,
	Integer sortOrder
) {
}
