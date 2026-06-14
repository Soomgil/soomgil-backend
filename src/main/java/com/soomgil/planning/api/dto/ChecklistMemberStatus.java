package com.soomgil.planning.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ChecklistMemberStatus(
	@Valid
	@NotNull
	UserSummary user,
	@NotNull
	Boolean isCompleted,
	OffsetDateTime completedAt,
	OffsetDateTime updatedAt
) {
}
