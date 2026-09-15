package com.soomgil.voting.application.port;

import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 투표 세션 persistence record.
 *
 * <p>{@code stickerAllowance}와 {@code selectionCount}는 투표 시작 후 변경할 수 없다.
 * {@code resultAppliedAt}이 채워져 있으면 결과가 이미 일정과 취향에 반영된 것이므로 재적용하지 않는다.
 *
 * @param id 세션 식별자
 * @param tripId 여행방 식별자
 * @param status 세션 상태
 * @param createdByUserId 투표를 시작한 방장
 * @param stickerAllowance 사용자당 스티커 지급 개수
 * @param selectionCount 최종 선정 관광지 개수
 * @param candidateCount 후보 관광지 수
 * @param openedAt 투표 시작 시각
 * @param completedAt 종료 시각. 진행 중이면 null
 * @param completionReason 종료 사유. 진행 중이면 null
 * @param completedByUserId 조기 종료를 실행한 방장. 자동 종료면 null
 * @param resultAppliedAt 결과 반영 시각. 아직 반영 전이면 null
 */
public record VoteSessionRecord(
	UUID id,
	UUID tripId,
	VoteSessionStatus status,
	UUID createdByUserId,
	int stickerAllowance,
	int selectionCount,
	int candidateCount,
	Instant openedAt,
	Instant completedAt,
	VoteCompletionReason completionReason,
	UUID completedByUserId,
	Instant resultAppliedAt
) {
}
