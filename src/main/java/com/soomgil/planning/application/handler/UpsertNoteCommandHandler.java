package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.event.NoteUpsertedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.domain.model.NoteRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.domain.policy.PlanningPolicy;
import com.soomgil.planning.infrastructure.persistence.mapper.NoteMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UpsertNoteCommand}를 처리한다.
 *
 * <p>{@code (tripId, scopeType, itineraryDayId)} 조합으로 활성 note가 있으면 UPDATE,
 * 없으면 새로 INSERT한다. 메모의 현재 version과 요청 baseVersion이 다르면 충돌로 거절한다.
 */
@Component
@Transactional
public class UpsertNoteCommandHandler implements CommandHandler<UpsertNoteCommand, PlanningMutationResponse> {

	private final NoteMapper noteMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public UpsertNoteCommandHandler(
		NoteMapper noteMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker,
		PlanningEventBroadcaster broadcaster
	) {
		this.noteMapper = noteMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
		this.broadcaster = broadcaster;
	}

	@Override
	public PlanningMutationResponse handle(UpsertNoteCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());
		PlanningPolicy.validateScopeDay(command.scopeType(), command.itineraryDayId());
		if (command.baseVersion() < 0) {
			throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
		}

		Instant now = Instant.now();
		Optional<NoteRecord> existing = noteMapper.findByTripScopeDay(
			command.tripId(), command.scopeType(), command.itineraryDayId());

		NoteRecord record;
		if (existing.isEmpty()) {
			if (command.baseVersion() != 0) {
				throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
			}
			UUID noteId = UUID.randomUUID();
			int inserted = noteMapper.insert(noteId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.content(), command.actorUserId(), now);
			if (inserted != 1) {
				throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
			}
			record = new NoteRecord(noteId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.content(), 1,
				command.actorUserId(), command.actorUserId(), null, null, now, now);
		} else {
			NoteRecord current = existing.get();
			int updated = noteMapper.updateContent(current.id(), command.content(),
				command.actorUserId(), now, command.baseVersion());
			if (updated != 1) {
				throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
			}
			record = new NoteRecord(current.id(), current.tripId(), current.scopeType(),
				current.itineraryDayId(), command.content(), command.baseVersion() + 1,
				current.createdByUserId(), command.actorUserId(),
				current.deletedByUserId(), current.deletedAt(), current.createdAt(), now);
		}

		Note dto = assembler.toNoteDto(record);
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new NoteUpsertedEvent(command.tripId(), command.actorUserId(), dto));
		return response;
	}
}
