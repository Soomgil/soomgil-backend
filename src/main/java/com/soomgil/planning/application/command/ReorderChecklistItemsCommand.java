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
 * per-item version check를 수행하며 하나라도 충돌하면 {@code @Transactional} rollback으로
 * 전체를 원상복구하고 {@link com.soomgil.global.error.ErrorCode#PLANNING_VERSION_CONFLICT}.
 *
 * @param tripId 여행방 식별자
 * @param checklistId 소속 checklist
 * @param actorUserId 요청자
 * @param baseVersion checklist의 version
 * @param itemOrders 각 item의 새 sortOrder 목록
 */
public record ReorderChecklistItemsCommand(
	UUID tripId,
	UUID checklistId,
	UUID actorUserId,
	long baseVersion,
	List<ChecklistItemOrder> itemOrders
) implements Command<PlanningMutationResponse> {
}
