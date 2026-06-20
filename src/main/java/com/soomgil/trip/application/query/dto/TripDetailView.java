package com.soomgil.trip.application.query.dto;

import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 상세 view.
 *
 * <p>멤버 목록은 현재 user profile 저장소와 연결되기 전까지 userId 기반 view로 반환한다.
 */
public record TripDetailView(
	UUID id,
	String title,
	String displayDestination,
	TripStatus status,
	TripAccessRole myRole,
	long itineraryVersion,
	Instant createdAt,
	UUID ownerUserId,
	List<TripMemberView> members,
	UUID retrippedFromPostId
) {

	public TripDetailView {
		members = members == null ? List.of() : List.copyOf(members);
	}
}
