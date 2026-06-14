package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChecklistItemOrder(
	@NotNull
	UUID itemId,
	@NotNull
	Integer sortOrder
) {
}
