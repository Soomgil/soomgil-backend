package com.soomgil.trip.application.command.dto;

import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 생성 결과.
 *
 * <p>응답 DTO를 직접 반환하지 않고 application 결과만 담는다. API 계층은 이 결과를
 * {@code TripDetail} 같은 HTTP 응답 모델로 변환한다.
 */
public record CreateTripResult(
	UUID tripId,
	UUID ownerUserId,
	String title,
	String displayDestination,
	TripStatus status,
	long itineraryVersion,
	Instant createdAt,
	UUID ownerMemberId
) {
}
