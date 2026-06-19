package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.DeleteChecklistItemCommand;
import com.soomgil.planning.application.event.ChecklistItemDeletedEvent;
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
 * {@link DeleteChecklistItemCommand}를 처리한다.
 *
 * <p>식별자로 item을 찾고 baseVersion 일치 시 soft delete. version 충돌 시
 * {@link ErrorCode#PLANNING_VERSION_CONFLICT}.
 *
 * <p>이 handler는 {@code @ResponseStatus(NO_CONTENT) void}로 반환되는 controller endpoint의
 * underlying command handler로 사용될 수도 있고, mutation 응답이 필요한 경우를 위해
 * {@link PlanningMutationResponse}를 반환한다. controller의 선택에 따라 무시 가능.
 */
@Component
@Transactional
public class DeleteChecklistItemCommandHandler implements CommandHandler<DeleteChecklistItemCommand, PlanningMutationResponse> {

	private final ChecklistItemMapper itemMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public DeleteChecklistItemCommandHandler(
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
	public PlanningMutationResponse handle(DeleteChecklistItemCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		ChecklistItemRecord record = itemMapper.findById(command.itemId())
			.filter(r -> !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_ITEM_NOT_FOUND));

		Instant now = Instant.now();
		int affected = itemMapper.softDelete(record.id(), command.baseVersion(), now);
		if (affected == 0) {
			throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
		}

		ChecklistItemRecord tombstone = new ChecklistItemRecord(record.id(), record.checklistId(),
			record.sortOrder(), record.content(), record.version() + 1, now,
			record.createdAt(), now);
		ChecklistItem dto = assembler.toItemDto(tombstone, List.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(),
			tombstone.version(), dto);
		broadcaster.broadcast(new ChecklistItemDeletedEvent(command.tripId(),
			command.actorUserId(), command.checklistId(), record.id(), tombstone.version()));
		return response;
	}
}
