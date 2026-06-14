package com.soomgil.user.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
	@NotNull
	UUID id,
	@Email
	String primaryEmail,
	OffsetDateTime primaryEmailVerifiedAt,
	@NotNull
	UserStatus status,
	String statusReason,
	OffsetDateTime deletionRequestedAt,
	OffsetDateTime deletionScheduledAt,
	@Valid
	@NotNull
	UserProfile profile,
	@Valid
	@NotNull
	UserSettings settings,
	@NotNull
	OffsetDateTime createdAt
) {
}
