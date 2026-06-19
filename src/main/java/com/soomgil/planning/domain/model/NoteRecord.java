package com.soomgil.planning.domain.model;

import com.soomgil.planning.api.dto.PlanningScopeType;
import java.time.Instant;
import java.util.UUID;

/**
 * planning.notes row.
 *
 * <p>trip의 특정 scope({@link PlanningScopeType#TRIP} 또는 {@link PlanningScopeType#DAY}) 단위로
 * 작성되는 자유 메모. {@code (tripId, scopeType, itineraryDayId)} 조합으로 unique하다.
 * TRIP scope는 {@code itineraryDayId}가 null이고, DAY scope는 필수다.
 *
 * <p>{@code version}은 낙관적 동시성 제어용. UPDATE/DELETE 시 {@code WHERE version = ?}로 검증한다.
 *
 * @param id note 식별자
 * @param tripId 여행방 식별자
 * @param scopeType 범위 (TRIP 또는 DAY)
 * @param itineraryDayId DAY scope인 경우 일차 식별자. TRIP scope이면 null
 * @param content 본문
 * @param version 낙관적 잠금 버전
 * @param deletedAt soft delete 시각. null이면 활성
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 수정 시각
 */
public record NoteRecord(
	UUID id,
	UUID tripId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
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

	/**
	 * 이 note가 주어진 scope/day와 일치하는지 검사.
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
