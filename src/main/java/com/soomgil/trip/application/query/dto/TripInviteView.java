package com.soomgil.trip.application.query.dto;

import com.soomgil.trip.domain.model.InviteStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 초대 API 응답으로 변환할 view.
 *
 * <p>token hash는 포함하지 않고, 사용자에게 노출 가능한 inviteCode만 담는다.
 */
public record TripInviteView(
	UUID id,
	UUID tripId,
	String inviteCode,
	UUID inviteeUserId,
	InviteStatus status,
	Instant expiresAt,
	Instant createdAt
) {
}
