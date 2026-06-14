package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportReason(
	@NotNull
	ReportReasonCode code,
	@NotBlank
	String displayName,
	Boolean isActive
) {
}
