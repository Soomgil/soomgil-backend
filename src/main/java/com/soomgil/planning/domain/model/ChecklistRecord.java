package com.soomgil.planning.domain.model;

import com.soomgil.planning.api.dto.PlanningScopeType;
import java.time.Instant;
import java.util.UUID;

/**
 * planning.checklists row.
 *
 * <p>trip의 특정 scope/day 단위로 존재하는 checklist. {@code (tripId, scopeType, itineraryDayId)}
 * 조합으로 unique하다. {@code title}은 표현용 필드이며 unique 대상이 아니다.
 *
 * <p>삭제 시 연결된 {@code checklist_items}도 cascade로 soft delete된다.
 *
 * @param id checklist 식별자
 * @param tripId 여행방 식별자
 * @param scopeType 범위 (TRIP 또는 DAY)
 * @param itineraryDayId DAY scope인 경우 일차 식별자. TRIP scope이면 null
 * @param title 표현용 제목 (nullable)
 * @param createdByUserId 최초 작성자
 * @param updatedByUserId 마지막 수정자
 * @param deletedByUserId 삭제자
 * @param deletedAt soft delete 시각. null이면 활성
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 수정 시각
 */
public record ChecklistRecord(
	UUID id,
	UUID tripId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String title,
	UUID createdByUserId,
	UUID updatedByUserId,
	UUID deletedByUserId,
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

	/**
	 * 이 checklist가 주어진 scope/day와 일치하는지 검사.
	 *
	 * @param scopeType 비교할 scope
	 * @param itineraryDayId 비교할 일차 식별자 (TRIP scope이면 null)
	 * @return scope과 day가 모두 일치하면 true
	 */
	public boolean matchesScope(PlanningScopeType scopeType, UUID itineraryDayId) {
		return this.scopeType == scopeType
			&& java.util.Objects.equals(this.itineraryDayId, itineraryDayId);
	}
}
