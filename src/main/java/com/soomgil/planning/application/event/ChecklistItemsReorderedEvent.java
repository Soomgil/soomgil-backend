package com.soomgil.planning.application.event;

import com.soomgil.planning.api.dto.Checklist;
import java.util.UUID;

/**
 * checklist item 일괄 재정렬 이벤트.
 *
 * <p>클라이언트는 {@code checklist}의 새 item 순서를 그대로 반영한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 재정렬 수행자
 * @param checklist 재정렬 결과 checklist DTO (items가 새 sortOrder로 정렬됨)
 */
public record ChecklistItemsReorderedEvent(
	UUID tripId,
	UUID actorUserId,
	Checklist checklist
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.items.reordered";
	}
}
