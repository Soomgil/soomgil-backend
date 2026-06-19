package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * 현재 사용자의 checklist item 완료 상태 토글 요청.
 *
 * <p>기존 멤버 상태 row가 있으면 {@code baseVersion} 검증 후 UPDATE.
 * 없으면(first touch) {@code version=1}로 INSERT.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param itemId item 식별자
 * @param actorUserId 요청자 (토글 주체)
 * @param baseVersion 기존 멤버 상태의 version. first touch 시 값은 무시됨
 * @param isCompleted 새 완료 여부
 */
public record UpdateChecklistMemberStatusCommand(
	UUID tripId,
	UUID checklistId,
	UUID itemId,
	UUID actorUserId,
	long baseVersion,
	boolean isCompleted
) implements Command<PlanningMutationResponse> {
}
