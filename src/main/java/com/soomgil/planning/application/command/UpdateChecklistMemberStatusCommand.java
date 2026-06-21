package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * 현재 사용자의 checklist item 완료 상태 토글 요청.
 *
 * <p>기존 멤버 상태 row가 있으면 UPDATE. 없으면(first touch) INSERT.
 * DBML에 version 컬럼이 없으므로 optimistic lock은 수행하지 않는다.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param checklistItemId item 식별자
 * @param actorUserId 요청자 (토글 주체)
 * @param isCompleted 새 완료 여부
 */
public record UpdateChecklistMemberStatusCommand(
	UUID tripId,
	UUID checklistId,
	UUID checklistItemId,
	UUID actorUserId,
	boolean isCompleted
) implements Command<PlanningMutationResponse> {
}
