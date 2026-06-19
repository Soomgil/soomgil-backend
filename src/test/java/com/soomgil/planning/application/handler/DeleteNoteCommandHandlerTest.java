package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.DeleteNoteCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.NoteRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.NoteMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeleteNoteCommandHandlerTest {

	private final NoteMapper noteMapper = mock(NoteMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final DeleteNoteCommandHandler handler = new DeleteNoteCommandHandler(
		noteMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("활성 note를 baseVersion 일치로 soft delete한다")
	void deletesActiveNote() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();
		NoteRecord existing = new NoteRecord(noteId, tripId, PlanningScopeType.TRIP, null,
			"본문", 2L, null, Instant.now(), Instant.now());

		when(noteMapper.findById(noteId)).thenReturn(Optional.of(existing));
		when(noteMapper.softDelete(eq(noteId), eq(2L), any())).thenReturn(1);
		Note stubNote = new Note(noteId, tripId, PlanningScopeType.TRIP, null, "본문", 3L, null);
		when(assembler.toNoteDto(any(NoteRecord.class))).thenReturn(stubNote);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 3L, null, false, false, stubNote, null, null, null);
		when(assembler.toMutationResponse(eq(tripId), eq(3L), any(Note.class))).thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new DeleteNoteCommand(
			tripId, noteId, actorId, 2L
		));

		assertThat(result).isSameAs(stubResponse);
		verify(noteMapper).softDelete(eq(noteId), eq(2L), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("식별자로 찾을 수 없으면 PLANNING_NOTE_NOT_FOUND")
	void notFoundThrows() {
		UUID tripId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();

		when(noteMapper.findById(noteId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new DeleteNoteCommand(
			tripId, noteId, UUID.randomUUID(), 1L
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_NOTE_NOT_FOUND));

		verify(noteMapper, never()).softDelete(any(), anyLong(), any());
	}

	@Test
	@DisplayName("soft delete 시 affectedRows가 0이면 PLANNING_VERSION_CONFLICT")
	void versionConflictThrows() {
		UUID tripId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();
		NoteRecord existing = new NoteRecord(noteId, tripId, PlanningScopeType.TRIP, null,
			"본문", 2L, null, Instant.now(), Instant.now());

		when(noteMapper.findById(noteId)).thenReturn(Optional.of(existing));
		when(noteMapper.softDelete(eq(noteId), eq(1L), any())).thenReturn(0);

		assertThatThrownBy(() -> handler.handle(new DeleteNoteCommand(
			tripId, noteId, UUID.randomUUID(), 1L
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_VERSION_CONFLICT));
	}
}
