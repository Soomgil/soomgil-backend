package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpsertNoteCommand;
import com.soomgil.planning.application.event.NoteUpsertedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.NoteRecord;
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
 * 없으면 새로 INSERT한다. DBML에 version 컬럼이 없으므로 optimistic lock은 수행하지 않는다.
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

		NoteRecord record;
		if (existing.isEmpty()) {
			UUID noteId = UUID.randomUUID();
			noteMapper.insert(noteId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.content(), command.actorUserId(), now);
			record = new NoteRecord(noteId, command.tripId(), command.scopeType(),
				command.itineraryDayId(), command.content(),
				command.actorUserId(), command.actorUserId(), null, null, now, now);
		} else {
			NoteRecord current = existing.get();
			noteMapper.updateContent(current.id(), command.content(),
				command.actorUserId(), now);
			record = new NoteRecord(current.id(), current.tripId(), current.scopeType(),
				current.itineraryDayId(), command.content(),
				current.createdByUserId(), command.actorUserId(),
				current.deletedByUserId(), current.deletedAt(), current.createdAt(), now);
		}

		Note dto = assembler.toNoteDto(record);
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new NoteUpsertedEvent(command.tripId(), command.actorUserId(), dto));
		return response;
	}
}
