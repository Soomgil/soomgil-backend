package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.ChecklistMemberStatus;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpdateChecklistMemberStatusCommand;
import com.soomgil.planning.application.event.ChecklistMemberStatusUpdatedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.ChecklistMemberStatusRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMemberStatusMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpdateChecklistMemberStatusCommand}를 처리한다.
 *
 * <p>현재 사용자의 checklist item 완료 상태를 토글한다.
 * 기존 member status row가 있으면 UPDATE, 없으면 first touch로 INSERT.
 * DBML에 version 컬럼이 없으므로 optimistic lock은 수행하지 않는다.
 */
@Component
@Transactional
public class UpdateChecklistMemberStatusCommandHandler implements CommandHandler<UpdateChecklistMemberStatusCommand, PlanningMutationResponse> {

	private final ChecklistItemMapper itemMapper;
	private final ChecklistMemberStatusMapper statusMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public UpdateChecklistMemberStatusCommandHandler(
		ChecklistItemMapper itemMapper,
		ChecklistMemberStatusMapper statusMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker,
		PlanningEventBroadcaster broadcaster
	) {
		this.itemMapper = itemMapper;
		this.statusMapper = statusMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
		this.broadcaster = broadcaster;
	}

	@Override
	public PlanningMutationResponse handle(UpdateChecklistMemberStatusCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		ChecklistItemRecord item = itemMapper.findById(command.checklistItemId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_ITEM_NOT_FOUND));

		Instant now = Instant.now();
		Instant completedAt = command.isCompleted() ? now : null;
		Optional<ChecklistMemberStatusRecord> existing = statusMapper.findByItemIdAndUserId(
			command.checklistItemId(), command.actorUserId());

		ChecklistMemberStatusRecord result;
		if (existing.isEmpty()) {
			statusMapper.insert(command.checklistItemId(), command.actorUserId(),
				command.isCompleted(), completedAt, command.actorUserId(), now);
			result = new ChecklistMemberStatusRecord(command.checklistItemId(),
				command.actorUserId(), command.isCompleted(), completedAt,
				command.actorUserId(), now);
		} else {
			statusMapper.updateStatus(command.checklistItemId(), command.actorUserId(),
				command.isCompleted(), completedAt, command.actorUserId(), now);
			result = new ChecklistMemberStatusRecord(command.checklistItemId(),
				command.actorUserId(), command.isCompleted(), completedAt,
				command.actorUserId(), now);
		}

		ChecklistMemberStatus dto = assembler.toMemberStatusDto(result);
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new ChecklistMemberStatusUpdatedEvent(command.tripId(),
			command.actorUserId(), command.checklistId(), command.checklistItemId(), dto));
		return response;
	}
}
