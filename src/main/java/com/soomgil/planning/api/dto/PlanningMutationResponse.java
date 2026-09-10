package com.soomgil.planning.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * planning mutation 응답.
 *
 * <p>메모와 체크리스트 변경은 MVP의 collaboration undo/redo 대상이 아니므로
 * {@code itineraryVersion}/{@code commandEventId}는 null이고 undo/redo flag는 false다.
 * 메모의 다음 변경 기준은 {@link Note#version()}으로 전달한다.
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
