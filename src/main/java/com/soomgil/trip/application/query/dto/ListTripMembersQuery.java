package com.soomgil.trip.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 멤버 목록을 조회하는 query.
 *
 * <p>요청 사용자가 active member일 때만 멤버 목록을 반환한다.
 */
public record ListTripMembersQuery(
	UUID tripId,
	UUID userId,
	TripMemberStatus status
) implements Query<List<TripMemberView>> {
}
