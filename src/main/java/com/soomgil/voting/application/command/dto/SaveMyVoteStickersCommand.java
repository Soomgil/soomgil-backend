package com.soomgil.voting.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.voting.api.dto.MyVoteStickerState;
import java.util.List;
import java.util.UUID;

/**
 * 스티커 배치 저장 command.
 *
 * <p>배치 전체를 치환하는 snapshot 방식이라 이동과 회수를 별도 command 없이 표현한다.
 * 제출을 마친 참여자는 {@code VOTE_ALREADY_SUBMITTED}로 거절된다.
 *
 * @param tripId 여행방 식별자
 * @param sessionId 세션 식별자
 * @param actorUserId 요청 사용자. 확정된 참여자여야 한다
 * @param placements 저장할 배치 전체
 */
public record SaveMyVoteStickersCommand(
	UUID tripId,
	UUID sessionId,
	UUID actorUserId,
	List<VoteStickerPlacementCommand> placements
) implements Command<MyVoteStickerState> {

	public SaveMyVoteStickersCommand {
		placements = placements == null ? List.of() : List.copyOf(placements);
	}
}
