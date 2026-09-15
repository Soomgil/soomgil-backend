package com.soomgil.voting.domain.policy;

import java.util.UUID;

/**
 * 후보별 스티커 집계와 선정 결과.
 *
 * <p>{@code selected}와 {@code selectedRank}는 {@link VoteResultSelectionPolicy}가 채운다.
 * 선정되지 않은 후보의 {@code selectedRank}는 null이다.
 *
 * <p>동점으로 함께 선정된 후보는 같은 {@code selectedRank}를 가진다. 따라서 선정 결과의 크기가
 * 방장이 정한 선정 개수보다 클 수 있다.
 *
 * @param candidateId 후보 식별자
 * @param sortOrder 투표 시작 시점 snapshot 순서. 스티커 수 동점일 때 정렬 기준으로 쓰인다
 * @param stickerCount 모든 참여자가 이 후보에 붙인 스티커 총합
 * @param selected 선정 여부
 * @param selectedRank 선정 순위. 선정되지 않았으면 null
 */
public record VoteCandidateTally(
	UUID candidateId,
	int sortOrder,
	int stickerCount,
	boolean selected,
	Integer selectedRank
) {

	/**
	 * 선정 결과를 적용한 사본을 만든다.
	 *
	 * @param rank 부여할 선정 순위
	 * @return 선정 표시가 적용된 사본
	 */
	public VoteCandidateTally selectedAt(int rank) {
		return new VoteCandidateTally(candidateId, sortOrder, stickerCount, true, rank);
	}
}
