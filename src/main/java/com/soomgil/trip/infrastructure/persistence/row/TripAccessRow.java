package com.soomgil.trip.infrastructure.persistence.row;

import java.util.UUID;

/**
 * 여행방 접근 권한 계산용 조인 row.
 *
 * <p>사용자의 membership row가 없으면 memberStatus는 null이다.
 */
public record TripAccessRow(
	UUID tripId,
	UUID userId,
	String tripStatus,
	String memberStatus,
	UUID ownerUserId
) {
}
