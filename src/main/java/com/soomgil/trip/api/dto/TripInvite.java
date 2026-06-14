package com.soomgil.trip.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripInvite(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotBlank
	String inviteCode,
	URI inviteUrl,
	UUID inviteeUserId,
	@NotNull
	InviteStatus status,
	OffsetDateTime expiresAt,
	@NotNull
	OffsetDateTime createdAt
) {
}
