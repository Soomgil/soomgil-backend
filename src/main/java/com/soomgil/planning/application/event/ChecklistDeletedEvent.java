package com.soomgil.planning.application.event;

import java.util.UUID;

/**
 * checklist soft delete 이벤트. cascade로 소속 item도 함께 삭제됐음을 의미한다.
 *
 * <p>클라이언트는 {@code checklistId}로 로컬 상태에서 해당 checklist와 종속 item들을 제거한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 삭제 수행자
 * @param checklistId 삭제된 checklist 식별자
 * @param version tombstone version
 */
public record ChecklistDeletedEvent(
	UUID tripId,
	UUID actorUserId,
	UUID checklistId,
	long version
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.deleted";
	}
}
