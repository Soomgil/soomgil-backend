package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 여행방 soft delete를 요청하는 command.
 */
public record DeleteTripCommand(
	UUID tripId,
	UUID actorUserId
) implements Command<NoResult> {
}
