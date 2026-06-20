package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.ChecklistItemOrder;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.List;
import java.util.UUID;

/**
 * checklist item 일괄 재정렬 요청.
 *
 * <p>한 checklist의 활성 item들을 {@code itemOrders}에 지정된 {@code sortOrder}로 재배치한다.
 * DBML에 version 컬럼이 없으므로 per-item version check는 수행하지 않는다.
 * {@code @Transactional}로 전체 재배치의 원자성을 보장한다.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param actorUserId 요청자
 * @param itemOrders 각 item의 새 sortOrder 목록
 */
public record ReorderChecklistItemsCommand(
	UUID tripId,
	UUID checklistId,
	UUID actorUserId,
	List<ChecklistItemOrder> itemOrders
) implements Command<PlanningMutationResponse> {
}
