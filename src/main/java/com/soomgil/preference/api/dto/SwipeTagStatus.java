package com.soomgil.preference.api.dto;

import java.util.List;

public record SwipeTagStatus(
	String externalPlaceId,
	List<String> tags,
	TagPreparationStatus status
) {
	public SwipeTagStatus {
		tags = tags == null ? List.of() : List.copyOf(tags);
	}
}
