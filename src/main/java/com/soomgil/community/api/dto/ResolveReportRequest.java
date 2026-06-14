package com.soomgil.community.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ResolveReportRequest(
	@NotBlank
	String status,
	String resolutionNote,
	@Valid
	CreateModerationActionRequest moderationAction
) {
}
