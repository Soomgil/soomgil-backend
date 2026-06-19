package com.soomgil.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * planning.checklist_item_member_status row.
 *
 * <p>각 사용자의 item 완료 상태. {@code (itemId, userId)} 복합 PK로 unique하다.
 * 첫 토글 시 INSERT({@code version=1}), 이후 UPDATE는 {@code WHERE version = ?}로 검증한다.
 *
 * @param itemId 대상 item 식별자
 * @param userId 완료 토글을 한 사용자
 * @param isCompleted 완료 여부
 * @param completedAt 완료 표시한 시각. {@code isCompleted=false}이면 null
 * @param version 낙관적 잠금 버전
 * @param updatedAt 마지막 수정 시각
 */
public record ChecklistMemberStatusRecord(
	UUID itemId,
	UUID userId,
	boolean isCompleted,
	Instant completedAt,
	long version,
	Instant updatedAt
) {
}
