package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContentReport(
	@NotNull
	UUID id,
	@Valid
	UserSummary reporter,
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	ReportReasonCode reasonCode,
	String detail,
	@NotNull
	ReportStatus status,
	@NotNull
	OffsetDateTime createdAt,
	OffsetDateTime resolvedAt,
	String resolutionNote
) {
}
