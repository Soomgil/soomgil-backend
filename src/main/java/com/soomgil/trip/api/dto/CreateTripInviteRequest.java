package com.soomgil.trip.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTripInviteRequest(
	UUID inviteeUserId,
	OffsetDateTime expiresAt
) {
}
