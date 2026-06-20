package com.soomgil.trip.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.trip.domain.model.InviteStatus;
import java.util.List;
import java.util.UUID;

/**
 * 여행방 초대 목록을 조회하는 query.
 *
 * <p>여행방 owner만 실행할 수 있다.
 */
public record ListTripInvitesQuery(
	UUID tripId,
	UUID actorUserId,
	InviteStatus status
) implements Query<List<TripInviteView>> {
}
