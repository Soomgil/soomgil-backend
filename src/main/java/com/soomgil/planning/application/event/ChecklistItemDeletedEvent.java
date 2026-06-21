package com.soomgil.planning.application.event;

import java.util.UUID;

/**
 * checklist item soft delete 이벤트.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 삭제 수행자
 * @param checklistId 소속 checklist 식별자
 * @param itemId 삭제된 item 식별자
 */
public record ChecklistItemDeletedEvent(
	UUID tripId,
	UUID actorUserId,
	UUID checklistId,
	UUID itemId
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.item.deleted";
	}
}
