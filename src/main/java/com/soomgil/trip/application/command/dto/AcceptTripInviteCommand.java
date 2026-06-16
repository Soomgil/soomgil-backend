package com.soomgil.trip.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * 여행방 초대 수락을 요청하는 command.
 *
 * <p>{@code inviteCode}는 사용자에게 노출된 초대 code이며, handler는 현재 사용자 ID로
 * 직접 초대 대상 여부와 기존 멤버십을 확인한다.
 */
public record AcceptTripInviteCommand(
	String inviteCode,
	UUID actorUserId
) implements Command<AcceptTripInviteResult> {
}
