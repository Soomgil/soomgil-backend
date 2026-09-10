package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.DeleteNoteCommand;
import com.soomgil.planning.application.event.NoteDeletedEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.NoteRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.NoteMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteNoteCommand}를 처리한다.
 *
 * <p>요청한 여행방에 속한 note만 soft delete한다. 찾을 수 없거나 이미 삭제된 note는
 * {@link ErrorCode#PLANNING_NOTE_NOT_FOUND}, 현재 version과 요청 baseVersion이 다르면
 * {@link ErrorCode#PLANNING_VERSION_CONFLICT}로 거절한다.
 */
@Component
@Transactional
public class DeleteNoteCommandHandler implements CommandHandler<DeleteNoteCommand, PlanningMutationResponse> {

	private final NoteMapper noteMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;
	private final PlanningEventBroadcaster broadcaster;

	public DeleteNoteCommandHandler(
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
	public PlanningMutationResponse handle(DeleteNoteCommand command) {
		accessChecker.requireMember(command.tripId(), command.actorUserId());

		NoteRecord record = noteMapper.findById(command.noteId())
			.filter(r -> r.tripId().equals(command.tripId()) && !r.isDeleted())
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_NOTE_NOT_FOUND));

		Instant now = Instant.now();
		int deleted = noteMapper.softDelete(record.id(), command.actorUserId(), now, command.baseVersion());
		if (deleted != 1) {
			throw new PlanningException(ErrorCode.PLANNING_VERSION_CONFLICT);
		}

		NoteRecord tombstone = new NoteRecord(record.id(), record.tripId(), record.scopeType(),
			record.itineraryDayId(), record.content(), command.baseVersion() + 1,
			record.createdByUserId(), command.actorUserId(), command.actorUserId(),
			now, record.createdAt(), now);
		Note dto = assembler.toNoteDto(tombstone);
		PlanningMutationResponse response = assembler.toMutationResponse(command.tripId(), dto);
		broadcaster.broadcast(new NoteDeletedEvent(command.tripId(), command.actorUserId(),
			record.id()));
		return response;
	}
}
