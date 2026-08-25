package com.soomgil.voting.api.dto;

/**
 * 방장 조기 종료 요청.
 *
 * <p>아직 제출하지 않은 참여자가 있는 상태에서 종료하려면 {@code acknowledgeUnvotedParticipants}가
 * true여야 한다. false이면 경고를 확인하지 않은 것으로 보고 거절한다.
 *
 * @param acknowledgeUnvotedParticipants 미투표 참여자가 있다는 경고를 확인했는지 여부
 */
public record CloseVoteSessionRequest(
	boolean acknowledgeUnvotedParticipants
) {
}
