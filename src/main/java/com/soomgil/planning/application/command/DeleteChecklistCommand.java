package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * checklist soft delete 요청.
 *
 * <p>checklist를 soft delete하고 연결된 모든 활성 item도 cascade로 soft delete한다.
 *
 * @param tripId 여행방 식별자
 * @param checklistId checklist 식별자
 * @param actorUserId 요청자
 */
public record DeleteChecklistCommand(
	UUID tripId,
	UUID checklistId,
	UUID actorUserId
) implements Command<PlanningMutationResponse> {
}
