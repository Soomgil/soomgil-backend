package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.DeleteChecklistCommand;
import com.soomgil.planning.application.event.ChecklistDeletedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteChecklistCommand}를 처리한다.
 *
 * <p>checklist를 soft delete하고 연결된 모든 활성 item도 cascade로 soft delete한다.
 * 찾을 수 없거나 이미 삭제된 checklist는 {@link ErrorCode#PLANNING_CHECKLIST_NOT_FOUND}.
 */
@Component
@Transactional
public class DeleteChecklistCommandHandler implements CommandHandler<DeleteChecklistCommand, PlanningMutationResponse> {

	private final ChecklistMapper checklistMapper;
	private final ChecklistItemMapper itemMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public DeleteChecklistCommandHandler(
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
	public PlanningMutationResponse handle(DeleteChecklistCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		ChecklistRecord record = checklistMapper.findById(command.checklistId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_CHECKLIST_NOT_FOUND));

		Instant now = Instant.now();
		checklistMapper.softDelete(record.id(), command.actorUserId(), now);
		itemMapper.softDeleteByChecklistId(record.id(), command.actorUserId(), now);

		ChecklistRecord tombstone = new ChecklistRecord(record.id(), record.tripId(),
			record.scopeType(), record.itineraryDayId(), record.title(),
			record.createdByUserId(), command.actorUserId(), command.actorUserId(),
			now, record.createdAt(), now);
		Checklist dto = assembler.toChecklistDto(tombstone, List.of(), Map.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new ChecklistDeletedEvent(command.tripId(),
			command.actorUserId(), record.id()));
		return response;
	}
}
