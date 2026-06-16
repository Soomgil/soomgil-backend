package com.soomgil.trip.infrastructure.persistence.row;

import java.time.Instant;
import java.util.UUID;

/**
 * 초대 수락 검증용 SQL row.
 *
 * <p>초대 상태와 여행방 상태를 한 번에 읽어 application handler에서 판단한다.
 */
public record TripInviteAcceptRow(
	UUID id,
	UUID tripId,
	String inviteCode,
	UUID inviteeUserId,
	String status,
	Instant expiresAt,
	UUID ownerUserId,
	String tripStatus
) {
}
