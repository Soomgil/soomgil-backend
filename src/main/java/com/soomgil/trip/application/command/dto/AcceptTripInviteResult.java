package com.soomgil.trip.application.command.dto;

import java.util.UUID;

/**
 * 여행방 초대 수락 결과.
 *
 * <p>API 계층은 반환된 tripId로 여행방 상세 view를 다시 조회해 응답한다.
 */
public record AcceptTripInviteResult(
	UUID tripId
) {
}
