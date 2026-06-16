package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.InviteStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 초대 조회용 read model.
 *
 * <p>token hash는 응답이나 application read model에 포함하지 않는다.
 */
public record TripInviteReadModel(
	UUID id,
	UUID tripId,
	String inviteCode,
	UUID inviteeUserId,
	InviteStatus status,
	Instant expiresAt,
	Instant createdAt
) {
}
