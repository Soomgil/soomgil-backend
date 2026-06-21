package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
import com.soomgil.planning.application.event.ChecklistItemCreatedEvent;
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
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateChecklistItemCommand}를 처리한다.
 *
 * <p>{@code sortOrder}가 null이면 checklist의 (max + 1)로 자동 설정한다.
 * checklist가 없거나 삭제됐으면 {@link ErrorCode#PLANNING_CHECKLIST_NOT_FOUND}.
 */
@Component
@Transactional
public class CreateChecklistItemCommandHandler implements CommandHandler<CreateChecklistItemCommand, PlanningMutationResponse> {

	private final ChecklistMapper checklistMapper;
	private final ChecklistItemMapper itemMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public CreateChecklistItemCommandHandler(
		ChecklistMapper checklistMapper,
		ChecklistItemMapper itemMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker,
		PlanningEventBroadcaster broadcaster
	) {
		this.checklistMapper = checklistMapper;
		this.itemMapper = itemMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
		this.broadcaster = broadcaster;
	}

	@Override
	public PlanningMutationResponse handle(CreateChecklistItemCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		ChecklistRecord checklist = checklistMapper.findById(command.checklistId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_CHECKLIST_NOT_FOUND));

		int sortOrder = command.sortOrder() != null
			? command.sortOrder()
			: nextSortOrder(command.checklistId());

		Instant now = Instant.now();
		UUID itemId = UUID.randomUUID();
		itemMapper.insert(itemId, command.checklistId(), sortOrder, command.content(),
			command.actorUserId(), now);

		ChecklistItemRecord created = new ChecklistItemRecord(itemId, command.checklistId(),
			sortOrder, command.content(), command.actorUserId(), command.actorUserId(),
			null, null, now, now);
		ChecklistItem dto = assembler.toItemDto(created, List.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new ChecklistItemCreatedEvent(command.tripId(),
			command.actorUserId(), command.checklistId(), dto));
		return response;
	}

	private int nextSortOrder(UUID checklistId) {
		Integer max = itemMapper.findMaxSortOrder(checklistId);
		return max == null ? 0 : max + 1;
	}
}
