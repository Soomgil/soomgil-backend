package com.soomgil.trip.application.command.dto;

import com.soomgil.trip.domain.model.InviteStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 초대 생성 결과.
 *
 * <p>초대 token 원문은 V1 응답에 포함하지 않고, code 기반 수락에 필요한 {@code inviteCode}만 반환한다.
 */
public record CreateTripInviteResult(
	UUID id,
	UUID tripId,
	String inviteCode,
	UUID inviteeUserId,
	InviteStatus status,
	Instant expiresAt,
	Instant createdAt
) {
}
