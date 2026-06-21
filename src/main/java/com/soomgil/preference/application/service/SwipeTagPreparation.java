package com.soomgil.preference.application.service;

import com.soomgil.preference.api.dto.TagPreparationStatus;
import java.util.List;

public record SwipeTagPreparation(List<String> tags, TagPreparationStatus status) {
	public SwipeTagPreparation {
		tags = tags == null ? List.of() : List.copyOf(tags);
	}
}
