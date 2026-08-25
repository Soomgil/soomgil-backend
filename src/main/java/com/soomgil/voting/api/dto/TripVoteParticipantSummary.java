package com.soomgil.voting.api.dto;

/**
 * 투표 참여 현황 요약.
 *
 * <p>개별 참여자가 무엇에 스티커를 붙였는지는 노출하지 않고 진행률만 보여준다.
 *
 * @param total 확정된 전체 참여자 수
 * @param submitted 제출을 마친 참여자 수
 */
public record TripVoteParticipantSummary(
	int total,
	int submitted
) {
}
