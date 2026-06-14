package com.soomgil.trip.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripMember(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@Valid
	@NotNull
	UserSummary user,
	@NotNull
	TripMemberRole role,
	@NotNull
	TripAccessRole accessRole,
	@NotNull
	TripMemberStatus status,
	@NotNull
	OffsetDateTime joinedAt
) {
}
