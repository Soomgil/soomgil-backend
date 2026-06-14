package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ModerationAction(
	@NotNull
	UUID id,
	@Valid
	UserSummary moderator,
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	@NotNull
	ModerationActionType action,
	ModerationStatus moderationStatus,
	String moderationReason,
	@NotNull
	OffsetDateTime createdAt
) {
}
