package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpdateChecklistItemCommand;
import com.soomgil.planning.application.event.ChecklistItemUpdatedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateChecklistItemCommand}를 처리한다.
 *
 * <p>{@code content}/{@code sortOrder}가 null이면 SQL COALESCE로 기존값을 유지한다.
 * 둘 중 하나 이상을 변경한다. member status 목록은 mutation 대상이 아니므로 빈 상태로 응답한다.
 * 조회 API를 통해 다시 fetch하는 것을 전제로 한다.
 */
@Component
@Transactional
public class UpdateChecklistItemCommandHandler implements CommandHandler<UpdateChecklistItemCommand, PlanningMutationResponse> {

	private final ChecklistItemMapper itemMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public UpdateChecklistItemCommandHandler(
		ChecklistItemMapper itemMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker,
		PlanningEventBroadcaster broadcaster
	) {
		this.itemMapper = itemMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
		this.broadcaster = broadcaster;
	}

	@Override
	public PlanningMutationResponse handle(UpdateChecklistItemCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		ChecklistItemRecord record = itemMapper.findById(command.itemId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_ITEM_NOT_FOUND));

		Instant now = Instant.now();
		itemMapper.update(command.itemId(), command.content(),
			command.sortOrder(), command.actorUserId(), now);

		Integer newSortOrder = command.sortOrder() != null ? command.sortOrder() : record.sortOrder();
		String newContent = command.content() != null ? command.content() : record.content();
		ChecklistItemRecord updated = new ChecklistItemRecord(record.id(), record.checklistId(),
			newSortOrder, newContent, record.createdByUserId(), command.actorUserId(),
			record.deletedByUserId(), record.deletedAt(), record.createdAt(), now);

		ChecklistItem dto = assembler.toItemDto(updated, List.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new ChecklistItemUpdatedEvent(command.tripId(),
			command.actorUserId(), command.checklistId(), dto));
		return response;
	}
}
