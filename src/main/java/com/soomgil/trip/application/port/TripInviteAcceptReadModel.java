package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 초대 수락 검증에 필요한 read model.
 *
 * <p>초대 row와 여행방 상태를 함께 읽어 수락 가능 여부를 한 handler에서 판단한다.
 */
public record TripInviteAcceptReadModel(
	UUID id,
	UUID tripId,
	String inviteCode,
	UUID inviteeUserId,
	InviteStatus status,
	Instant expiresAt,
	UUID ownerUserId,
	TripStatus tripStatus
) {
}
