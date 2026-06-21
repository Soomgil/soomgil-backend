package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.UUID;

/**
 * checklist item 부분 수정 요청.
 *
 * <p>{@code content}/{@code sortOrder}가 null이면 기존값을 유지한다(SQL COALESCE).
 * 둘 중 하나 이상을 변경한다.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param itemId item 식별자
 * @param actorUserId 요청자
 * @param content 새 본문 (null이면 유지)
 * @param sortOrder 새 정렬 순서 (null이면 유지)
 */
public record UpdateChecklistItemCommand(
	UUID tripId,
	UUID checklistId,
	UUID itemId,
	UUID actorUserId,
	String content,
	Integer sortOrder
) implements Command<PlanningMutationResponse> {
}
