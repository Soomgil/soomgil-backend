package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateContentReportRequest(
	@NotNull
	ReportTargetType targetType,
	@NotNull
	UUID targetId,
	@NotNull
	ReportReasonCode reasonCode,
	@Size(max = 2000)
	String detail
) {
}
