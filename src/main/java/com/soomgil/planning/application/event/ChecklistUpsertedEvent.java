package com.soomgil.planning.application.event;

import com.soomgil.planning.api.dto.Checklist;
import java.util.UUID;

/**
 * checklist 생성 또는 title 갱신 이벤트.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 변경 수행자
 * @param checklist mutation 결과 checklist DTO
 */
public record ChecklistUpsertedEvent(
	UUID tripId,
	UUID actorUserId,
	Checklist checklist
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.upserted";
	}
}
