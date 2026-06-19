package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * checklist item soft delete 요청.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param itemId item 식별자
 * @param actorUserId 요청자
 * @param baseVersion item의 version
 */
public record DeleteChecklistItemCommand(
	UUID tripId,
	UUID checklistId,
	UUID itemId,
	UUID actorUserId,
	long baseVersion
) implements Command<PlanningMutationResponse> {
}
