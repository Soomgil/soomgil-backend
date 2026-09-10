package com.soomgil.planning.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

/**
 * 메모 생성 또는 수정 요청.
 *
 * @param baseVersion 클라이언트가 마지막으로 읽은 메모 버전. 신규 메모는 0
 * @param scopeType 여행 전체 또는 일차 범위
 * @param itineraryDayId DAY 범위일 때의 일차 식별자
 * @param content 메모 본문
 */
public record UpsertNoteRequest(
	@NotNull
	@PositiveOrZero
	Long baseVersion,
	@NotNull
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	@NotBlank
	String content
) {
}
