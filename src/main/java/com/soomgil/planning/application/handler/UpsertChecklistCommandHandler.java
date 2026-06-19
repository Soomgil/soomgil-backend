package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.event.ChecklistUpsertedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.model.PlanningException;
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
 * <p>(tripId, scopeType, itineraryDayId) 조합으로 활성 checklist가 있으면 baseVersion 검증 후
 * title UPDATE, 없으면 새로 INSERT한다. 신규 checklist는 item이 없으므로 빈 item 목록으로 응답한다.
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

		ChecklistRecord resultRecord;
		if (existing.isEmpty()) {
			UUID checklistId = UUID.randomUUID();
			checklistMapper.insert(checklistId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.title(), now);
			resultRecord = new ChecklistRecord(checklistId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.title(), 1L, null, now, now);
		} else {
			ChecklistRecord record = existing.get();
			int affected = checklistMapper.updateTitle(record.id(), command.title(),
				command.baseVersion(), now);
			if (affected == 0) {
				throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
			}
			resultRecord = new ChecklistRecord(record.id(), record.tripId(), record.scopeType(),
				record.itineraryDayId(), command.title(), record.version() + 1, null,
				record.createdAt(), now);
		}

		Checklist dto = assembler.toChecklistDto(resultRecord, List.of(), Map.of());
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(),
			resultRecord.version(), dto);
		broadcaster.broadcast(new ChecklistUpsertedEvent(command.tripId(),
			command.actorUserId(), dto));
		return response;
	}
}
