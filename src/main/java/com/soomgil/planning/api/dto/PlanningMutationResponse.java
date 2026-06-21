package com.soomgil.planning.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * planning mutation 응답.
 *
 * <p>{@code itineraryVersion}은 collaboration 모듈 연동 전까지 stub(null).
 * DBML planning 스키마에는 version 컬럼이 없으므로 resource 단위 version은 노출하지 않는다.
 * collaboration 도입 시 상위 itinerary_version으로 채워진다.
 */
public record PlanningMutationResponse(
	@NotNull
	UUID tripId,
	Long itineraryVersion,
	Long commandEventId,
	@NotNull
	Boolean undoAvailable,
	@NotNull
	Boolean redoAvailable,
	@Valid
	Note note,
	@Valid
	Checklist checklist,
	@Valid
	ChecklistItem item,
	@Valid
	ChecklistMemberStatus memberStatus
) {
}
