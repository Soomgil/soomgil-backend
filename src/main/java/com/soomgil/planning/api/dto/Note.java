package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행방 또는 일차 범위의 공동 메모.
 *
 * @param id 메모 식별자
 * @param tripId 여행방 식별자
 * @param scopeType 여행 전체 또는 일차 범위
 * @param itineraryDayId DAY 범위일 때의 일차 식별자
 * @param content 메모 본문
 * @param version 저장과 삭제 시 충돌 검사에 사용하는 메모 버전
 * @param deletedAt soft delete 시각
 */
public record Note(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	@NotNull
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	@NotBlank
	String content,
	long version,
	OffsetDateTime deletedAt
) {
}
