package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 여행방 멤버 제거를 요청하는 command.
 */
public record RemoveTripMemberCommand(
	UUID tripId,
	UUID targetUserId,
	UUID actorUserId
) implements Command<NoResult> {
}
