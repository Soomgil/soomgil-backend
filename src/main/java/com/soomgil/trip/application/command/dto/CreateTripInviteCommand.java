package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.time.Instant;
import java.util.UUID;

/**
 * 여행방 초대 생성을 요청하는 command.
 *
 * <p>여행방 owner만 실행할 수 있다. {@code inviteeUserId}가 null이면 링크/code 초대다.
 */
public record CreateTripInviteCommand(
	UUID tripId,
	UUID actorUserId,
	UUID inviteeUserId,
	Instant expiresAt
) implements Command<CreateTripInviteResult> {
}
