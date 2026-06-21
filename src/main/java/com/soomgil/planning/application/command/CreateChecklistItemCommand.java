package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * checklist item 추가 요청.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param actorUserId 요청자
 * @param content 본문
 * @param sortOrder 정렬 순서. null이면 (max + 1)로 자동 설정
 */
public record CreateChecklistItemCommand(
	UUID tripId,
	UUID checklistId,
	UUID actorUserId,
	String content,
	Integer sortOrder
) implements Command<PlanningMutationResponse> {
}
