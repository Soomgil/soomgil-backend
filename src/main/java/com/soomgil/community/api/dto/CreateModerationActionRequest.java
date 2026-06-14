package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateModerationActionRequest(
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	@NotNull
	ModerationActionType action,
	String moderationReason
) {
}
