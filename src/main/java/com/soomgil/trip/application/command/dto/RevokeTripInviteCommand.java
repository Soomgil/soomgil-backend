package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/**
 * 대기 중인 여행방 초대 취소를 요청하는 command.
 *
 * <p>여행방 owner만 실행할 수 있다.
 */
public record RevokeTripInviteCommand(
	UUID tripId,
	UUID inviteId,
	UUID actorUserId
) implements Command<NoResult> {
}
