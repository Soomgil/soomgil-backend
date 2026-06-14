package com.soomgil.ai.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiChatMessage(
	@NotNull
	UUID id,
	@NotNull
	AiMessageRole role,
	@Valid
	UserSummary requester,
	@NotBlank
	String content,
	UUID toolCallId,
	@NotNull
	OffsetDateTime createdAt
) {
}
