package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.UUID;

/**
 * 접근 권한 계산에 필요한 저장소 snapshot.
 *
 * <p>{@code memberStatus}는 요청 사용자의 membership row가 없으면 null일 수 있다.
 * {@code ownerUserId}는 여행방 row의 source of truth다.
 */
public record TripAccessSnapshot(
	UUID tripId,
	UUID userId,
	TripStatus tripStatus,
	TripMemberStatus memberStatus,
	UUID ownerUserId
) {
}
