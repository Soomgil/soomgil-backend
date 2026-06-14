package com.soomgil.ai.api.dto;

import com.soomgil.geo.api.dto.Viewport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAiMessageRequest(
	@NotBlank
	@Size(min = 1, max = 4000)
	String content,
	Long baseVersion,
	@Valid
	Viewport viewport
) {
}
