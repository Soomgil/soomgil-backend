package com.soomgil.planning.application.event;

import com.soomgil.planning.api.dto.ChecklistItem;
import java.util.UUID;

/**
 * checklist item 부분 수정(content/sortOrder) 이벤트.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 수정 수행자
 * @param checklistId 소속 checklist 식별자
 * @param item 수정된 item DTO
 */
public record ChecklistItemUpdatedEvent(
	UUID tripId,
	UUID actorUserId,
	UUID checklistId,
	ChecklistItem item
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.item.updated";
	}
}
