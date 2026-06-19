package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.event.NoteUpsertedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
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
 * <p>(tripId, scopeType, itineraryDayId) 조합으로 활성 note가 있으면 baseVersion 검증 후
 * UPDATE, 없으면 새로 INSERT한다. INSERT는 version=1로 시작한다.
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

		Instant now = Instant.now();
		Optional<NoteRecord> existing = noteMapper.findByTripScopeDay(
			command.tripId(), command.scopeType(), command.itineraryDayId());

		if (existing.isEmpty()) {
			UUID noteId = UUID.randomUUID();
			noteMapper.insert(noteId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.content(), now);
			NoteRecord created = new NoteRecord(noteId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.content(), 1L, null, now, now);
			Note dto = assembler.toNoteDto(created);
			PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), 1L, dto);
			broadcaster.broadcast(new NoteUpsertedEvent(command.tripId(), command.actorUserId(), dto));
			return response;
		}

		NoteRecord record = existing.get();
		int affected = noteMapper.updateContent(record.id(), command.content(),
			command.baseVersion(), now);
		if (affected == 0) {
			throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
		}
		NoteRecord updated = new NoteRecord(record.id(), record.tripId(), record.scopeType(),
			record.itineraryDayId(), command.content(), record.version() + 1, null,
			record.createdAt(), now);
		Note dto = assembler.toNoteDto(updated);
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), updated.version(), dto);
		broadcaster.broadcast(new NoteUpsertedEvent(command.tripId(), command.actorUserId(), dto));
		return response;
	}
}
