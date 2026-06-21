package com.soomgil.planning.application.event;

import com.soomgil.planning.api.dto.ChecklistMemberStatus;
import java.util.UUID;

/**
 * checklist item 멤버 완료 상태 토글 이벤트.
 *
 * <p>{@code actorUserId} 본인 외의 클라이언트는 이 이벤트로 해당 item의 member status 목록에
 * 새 항목을 추가하거나 기존 항목을 갱신한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 토글 수행자
 * @param checklistId 소속 checklist 식별자
 * @param itemId 대상 item 식별자
 * @param memberStatus mutation 결과 member status DTO
 */
public record ChecklistMemberStatusUpdatedEvent(
	UUID tripId,
	UUID actorUserId,
	UUID checklistId,
	UUID itemId,
	ChecklistMemberStatus memberStatus
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.checklist.member_status.updated";
	}
}
