package com.soomgil.notification.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Notification(
	@NotNull
	UUID id,
	@Valid
	UserSummary actor,
	UUID tripId,
	@NotBlank
	String type,
	@NotBlank
	String title,
	String body,
	@Valid
	@NotNull
	TripInviteNotificationPayload payload,
	OffsetDateTime readAt,
	@NotNull
	OffsetDateTime createdAt
) {
}
