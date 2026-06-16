package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 멤버 조회용 read model.
 *
 * <p>{@code ownerUserId}는 API 응답에 OWNER access role을 파생하기 위해 함께 전달한다.
 */
public record TripMemberReadModel(
	UUID id,
	UUID tripId,
	UUID userId,
	TripMemberRole role,
	TripMemberStatus status,
	Instant joinedAt,
	UUID ownerUserId
) {
}
