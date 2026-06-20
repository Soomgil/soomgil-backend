package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.event.ChecklistUpsertedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.policy.PlanningPolicy;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpsertChecklistCommand}를 처리한다.
 *
 * <p>{@code (tripId, scopeType, itineraryDayId)} 조합으로 활성 checklist가 있으면 title UPDATE,
 * 없으면 새로 INSERT한다. 신규 checklist는 item이 없으므로 빈 item 목록으로 응답한다.
 * DBML에 version 컬럼이 없으므로 optimistic lock은 수행하지 않는다.
 */
@Component
@Transactional
public class UpsertChecklistCommandHandler implements CommandHandler<UpsertChecklistCommand, PlanningMutationResponse> {

	private final ChecklistMapper checklistMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public UpsertChecklistCommandHandler(
		ChecklistMapper checklistMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker,
		PlanningEventBroadcaster broadcaster
	) {
		this.checklistMapper = checklistMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
		this.broadcaster = broadcaster;
	}

	@Override
	public PlanningMutationResponse handle(UpsertChecklistCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());
		PlanningPolicy.validateScopeDay(command.scopeType(), command.itineraryDayId());

		Instant now = Instant.now();
		Optional<ChecklistRecord> existing = checklistMapper.findByTripScopeDay(
			command.tripId(), command.scopeType(), command.itineraryDayId());

		ChecklistRecord record;
		if (existing.isEmpty()) {
			UUID checklistId = UUID.randomUUID();
			checklistMapper.insert(checklistId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.title(), command.actorUserId(), now);
			record = new ChecklistRecord(checklistId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.title(),
				command.actorUserId(), command.actorUserId(), null, null, now, now);
		} else {
			ChecklistRecord current = existing.get();
			checklistMapper.updateTitle(current.id(), command.title(),
				command.actorUserId(), now);
			record = new ChecklistRecord(current.id(), current.tripId(), current.scopeType(),
				current.itineraryDayId(), command.title(),
				current.createdByUserId(), command.actorUserId(),
				current.deletedByUserId(), current.deletedAt(), current.createdAt(), now);
		}

		Checklist dto = assembler.toChecklistDto(record, List.of(), Map.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new ChecklistUpsertedEvent(command.tripId(),
			command.actorUserId(), dto));
		return response;
	}
}
