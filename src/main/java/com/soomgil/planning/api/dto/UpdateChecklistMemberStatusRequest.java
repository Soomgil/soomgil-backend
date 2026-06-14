package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateChecklistMemberStatusRequest(
	@NotNull
	Long baseVersion,
	@NotNull
	Boolean isCompleted
) {
}
