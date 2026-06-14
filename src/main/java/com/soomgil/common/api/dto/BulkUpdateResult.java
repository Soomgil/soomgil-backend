package com.soomgil.common.api.dto;

import jakarta.validation.constraints.NotNull;

public record BulkUpdateResult(
	@NotNull
	Integer updatedCount
) {
}
