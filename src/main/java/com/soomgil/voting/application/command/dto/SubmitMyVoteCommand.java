package com.soomgil.voting.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.voting.api.dto.TripVoteSessionState;
import java.util.List;
import java.util.UUID;

/**
 * 투표 제출 command.
 *
 * <p>제출 후에는 스티커를 수정할 수 없다. 이 제출로 모든 활성 참여자가 제출을 마치면
 * handler가 같은 transaction 안에서 세션을 자동 종료하고 결과를 확정한다.
 *
 * @param tripId 여행방 식별자
 * @param sessionId 세션 식별자
 * @param actorUserId 요청 사용자
 * @param placements 제출과 함께 저장할 배치. null이면 저장된 배치를 그대로 제출한다
 */
public record SubmitMyVoteCommand(
	UUID tripId,
	UUID sessionId,
	UUID actorUserId,
	List<VoteStickerPlacementCommand> placements
) implements Command<TripVoteSessionState> {
}
