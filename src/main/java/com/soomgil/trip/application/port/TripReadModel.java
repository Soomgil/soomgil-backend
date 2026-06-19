package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 조회용 read model.
 *
 * <p>API DTO가 아니라 persistence에서 application query handler로 전달하는 중간 모델이다.
 */
public record TripReadModel(
	UUID id,
	UUID ownerUserId,
	String title,
	String displayDestination,
	TripStatus status,
	long itineraryVersion,
	Instant createdAt,
	UUID retrippedFromPostId
) {
}
