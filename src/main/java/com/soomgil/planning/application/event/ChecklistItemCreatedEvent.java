package com.soomgil.planning.application.event;

import com.soomgil.planning.api.dto.ChecklistItem;
import java.util.UUID;

/**
 * checklist item 추가 이벤트.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 추가 수행자
 * @param checklistId 소속 checklist 식별자
 * @param item 생성된 item DTO (member statuses는 빈 목록)
 */
public record ChecklistItemCreatedEvent(
	UUID tripId,
	UUID actorUserId,
	UUID checklistId,
	ChecklistItem item
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.item.created";
	}
}
