package com.soomgil.trip.application.query.dto;

import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 멤버 목록 view.
 *
 * <p>{@code accessRole}은 저장된 member role이 아니라 trip.ownerUserId에서 파생한 role이다.
 */
public record TripMemberView(
	UUID id,
	UUID tripId,
	UUID userId,
	TripMemberRole role,
	TripAccessRole accessRole,
	TripMemberStatus status,
	Instant joinedAt
) {
}
