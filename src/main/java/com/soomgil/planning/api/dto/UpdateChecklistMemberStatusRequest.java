package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateChecklistMemberStatusRequest(
	@NotNull
	Boolean isCompleted
) {
}
