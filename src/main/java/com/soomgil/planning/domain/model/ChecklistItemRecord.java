package com.soomgil.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * planning.checklist_items row.
 *
 * <p>하나의 checklist에 속한 항목. {@code sortOrder}로 정렬 순서를 보존한다.
 * 삭제된 item은 reorder 및 목록 응답에서 제외된다.
 *
 * @param id item 식별자
 * @param checklistId 소속 checklist 식별자
 * @param sortOrder 정렬 순서 (0-based)
 * @param content 항목 본문
 * @param version 낙관적 잠금 버전
 * @param deletedAt soft delete 시각. null이면 활성
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 수정 시각
 */
public record ChecklistItemRecord(
	UUID id,
	UUID checklistId,
	int sortOrder,
	String content,
	long version,
	Instant deletedAt,
	Instant createdAt,
	Instant updatedAt
) {

	/**
	 * 삭제 여부.
	 *
	 * @return deletedAt이 설정됐으면 true
	 */
	public boolean isDeleted() {
		return deletedAt != null;
	}
}
