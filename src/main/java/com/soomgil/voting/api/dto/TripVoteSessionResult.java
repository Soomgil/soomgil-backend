package com.soomgil.voting.api.dto;

import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 투표 종료 결과 응답.
 *
 * <p>선정된 관광지만 일차 미정 일정에 추가되며, 이미 일정에 있던 관광지는 중복 추가되지 않는다.
 * 같은 요청을 재시도해도 결과가 달라지지 않는 멱등 응답이다.
 *
 * @param sessionId 세션 식별자
 * @param tripId 여행방 식별자
 * @param status 세션 상태. 항상 {@code COMPLETED}
 * @param completionReason 종료 사유
 * @param completedAt 종료 시각
 * @param selectionCount 방장이 정한 선정 개수
 * @param results 후보별 결과. 스티커 총합 내림차순
 * @param unscheduledDayId 선정 결과가 추가된 일차 미정 그룹 식별자. 추가된 항목이 없으면 null
 * @param itineraryVersion 일정 반영 후 협업 version
 */
public record TripVoteSessionResult(
	@NotNull
	UUID sessionId,
	@NotNull
	UUID tripId,
	@NotNull
	VoteSessionStatus status,
	VoteCompletionReason completionReason,
	OffsetDateTime completedAt,
	int selectionCount,
	@Valid
	@NotNull
	List<TripVoteResultItem> results,
	UUID unscheduledDayId,
	Long itineraryVersion
) {

	public TripVoteSessionResult {
		results = results == null ? List.of() : List.copyOf(results);
	}
}
