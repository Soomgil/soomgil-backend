package com.soomgil.voting.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import java.util.UUID;

/**
 * 방장 조기 종료 command.
 *
 * <p>미투표 참여자가 있으면 {@code acknowledgeUnvotedParticipants}가 true여야 실행된다.
 * 이미 종료된 세션에 대해 호출하면 실패가 아니라 같은 결과를 그대로 반환하는 멱등 연산이다.
 *
 * @param tripId 여행방 식별자
 * @param sessionId 세션 식별자
 * @param actorUserId 요청 사용자. 방장이어야 한다
 * @param acknowledgeUnvotedParticipants 미투표자 경고 확인 여부
 */
public record CloseVoteSessionCommand(
	UUID tripId,
	UUID sessionId,
	UUID actorUserId,
	boolean acknowledgeUnvotedParticipants
) implements Command<TripVoteSessionResult> {
}
