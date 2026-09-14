package com.soomgil.voting.api.dto;

import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 투표 세션 상세 응답.
 *
 * <p>{@code stickerAllowance}와 {@code selectionCount}는 투표 시작 후 변경할 수 없다.
 *
 * @param id 세션 식별자
 * @param tripId 여행방 식별자
 * @param status 세션 상태
 * @param stickerAllowance 사용자당 스티커 지급 개수
 * @param selectionCount 최종 선정 관광지 개수
 * @param candidateCount 후보 관광지 수
 * @param openedAt 투표 시작 시각
 * @param completedAt 종료 시각. 진행 중이면 null
 * @param completionReason 종료 사유. 진행 중이면 null
 * @param participantSummary 참여 현황 요약
 * @param candidates 후보 목록
 * @param regions 후보를 뽑은 지역 목록. 투표 시작 시점의 snapshot이다
 */
public record TripVoteSessionDetail(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotNull
	VoteSessionStatus status,
	int stickerAllowance,
	int selectionCount,
	int candidateCount,
	OffsetDateTime openedAt,
	OffsetDateTime completedAt,
	VoteCompletionReason completionReason,
	@Valid
	TripVoteParticipantSummary participantSummary,
	@Valid
	@NotNull
	List<TripVoteCandidate> candidates,
	@Valid
	@NotNull
	List<TripVoteRegion> regions
) {

	public TripVoteSessionDetail {
		candidates = candidates == null ? List.of() : List.copyOf(candidates);
		regions = regions == null ? List.of() : List.copyOf(regions);
	}
}
