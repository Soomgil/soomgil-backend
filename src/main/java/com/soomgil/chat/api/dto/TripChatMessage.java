package com.soomgil.chat.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripChatMessage(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@Valid
	@NotNull
	UserSummary sender,
	String content,
	OffsetDateTime deletedAt,
	@NotNull
	OffsetDateTime createdAt
) {
}
