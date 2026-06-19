package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.ChecklistItemOrder;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.ReorderChecklistItemsCommand;
import com.soomgil.planning.application.event.ChecklistItemsReorderedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ReorderChecklistItemsCommand}를 처리한다.
 *
 * <p>한 checklist의 활성 item을 {@code itemOrders}에 지정된 sortOrder로 일괄 재배치한다.
 * 각 item은 checklist 소속이어야 하며, per-item version check를 수행한다.
 * 하나라도 충돌하면 {@code @Transactional} rollback으로 전체를 원상복구하고
 * {@link ErrorCode#PLANNING_VERSION_CONFLICT}.
 *
 * <p>응답의 member status 목록은 mutation 대상이 아니므로 생략. 조회 API로 다시 fetch한다.
 */
@Component
@Transactional
public class ReorderChecklistItemsCommandHandler implements CommandHandler<ReorderChecklistItemsCommand, PlanningMutationResponse> {

	private final ChecklistItemMapper itemMapper;
	private final ChecklistMapper checklistMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public ReorderChecklistItemsCommandHandler(
		ChecklistItemMapper itemMapper,
		ChecklistMapper checklistMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker,
		PlanningEventBroadcaster broadcaster
	) {
		this.itemMapper = itemMapper;
		this.checklistMapper = checklistMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
		this.broadcaster = broadcaster;
	}

	@Override
	public PlanningMutationResponse handle(ReorderChecklistItemsCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		ChecklistRecord checklist = checklistMapper.findById(command.checklistId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_CHECKLIST_NOT_FOUND));

		List<ChecklistItemRecord> items = command.itemOrders().stream()
			.map(order -> loadAndValidateItem(order, command.checklistId()))
			.toList();

		Instant now = Instant.now();
		for (int i = 0; i < items.size(); i++) {
			ChecklistItemRecord item = items.get(i);
			ChecklistItemOrder order = command.itemOrders().get(i);
			int affected = itemMapper.updateSortOrder(item.id(), order.sortOrder(),
				item.version(), now);
			if (affected == 0) {
				throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
			}
		}

		List<ChecklistItemRecord> refreshed = itemMapper.findByChecklistId(command.checklistId());
		Checklist dto = assembler.toChecklistDto(checklist, refreshed, Map.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(),
			checklist.version(), dto);
		broadcaster.broadcast(new ChecklistItemsReorderedEvent(command.tripId(),
			command.actorUserId(), dto));
		return response;
	}

	private ChecklistItemRecord loadAndValidateItem(ChecklistItemOrder order, UUID checklistId) {
		ChecklistItemRecord item = itemMapper.findById(order.itemId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_ITEM_NOT_FOUND));
		if (!item.checklistId().equals(checklistId)) {
			throw new PlanningException(ErrorCode.PLANNING_ITEM_NOT_FOUND);
		}
		return item;
	}
}
