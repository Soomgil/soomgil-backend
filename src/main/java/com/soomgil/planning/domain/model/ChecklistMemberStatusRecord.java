package com.soomgil.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * planning.checklist_item_member_statuses row.
 *
 * <p>각 사용자의 checklist item 완료 상태. {@code (checklistItemId, userId)} 복합 PK로 unique하다.
 * 첫 토글 시 INSERT, 이후 UPDATE는 {@code WHERE checklist_item_id = ? AND user_id = ?}로 식별한다.
 *
 * @param checklistItemId 대상 item 식별자
 * @param userId 완료 토글을 한 사용자
 * @param isCompleted 완료 여부
 * @param completedAt 완료 표시한 시각. {@code isCompleted=false}이면 null
 * @param updatedByUserId 마지막 상태 변경자 (보통 user_id와 동일)
 * @param updatedAt 마지막 수정 시각
 */
public record ChecklistMemberStatusRecord(
	UUID checklistItemId,
	UUID userId,
	boolean isCompleted,
	Instant completedAt,
	UUID updatedByUserId,
	Instant updatedAt
) {
}
