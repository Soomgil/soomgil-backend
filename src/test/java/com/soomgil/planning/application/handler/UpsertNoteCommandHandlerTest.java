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
import com.soomgil.planning.application.command.UpsertNoteCommand;
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

class UpsertNoteCommandHandlerTest {

	private final NoteMapper noteMapper = mock(NoteMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final UpsertNoteCommandHandler handler = new UpsertNoteCommandHandler(
		noteMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("신규 (trip, scope, day) 조합이면 INSERT하고 version=1로 응답한다")
	void insertsWhenAbsent() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();

		when(noteMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.empty());
		Note stubNote = new Note(UUID.randomUUID(), tripId, PlanningScopeType.TRIP, null, "본문", 1L, null);
		when(assembler.toNoteDto(any(NoteRecord.class))).thenReturn(stubNote);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 1L, null, false, false, stubNote, null, null, null);
		when(assembler.toMutationResponse(eq(tripId), eq(1L), any(Note.class))).thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpsertNoteCommand(
			tripId, actorId, 0L, PlanningScopeType.TRIP, null, "본문"
		));

		assertThat(result).isSameAs(stubResponse);
		verify(noteMapper).insert(any(UUID.class), eq(tripId), eq(PlanningScopeType.TRIP),
			eq(null), eq("본문"), any(Instant.class));
		verify(noteMapper, never()).updateContent(any(), any(), anyLong(), any());
		verify(accessChecker).requireMember(tripId, actorId);
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("기존 note가 있으면 baseVersion 검증 후 UPDATE한다")
	void updatesWhenPresent() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();
		NoteRecord existing = new NoteRecord(noteId, tripId, PlanningScopeType.TRIP, null,
			"이전 본문", 3L, null, Instant.now(), Instant.now());

		when(noteMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.of(existing));
		when(noteMapper.updateContent(eq(noteId), eq("새 본문"), eq(3L), any(Instant.class)))
			.thenReturn(1);
		Note stubNote = new Note(noteId, tripId, PlanningScopeType.TRIP, null, "새 본문", 4L, null);
		when(assembler.toNoteDto(any(NoteRecord.class))).thenReturn(stubNote);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 4L, null, false, false, stubNote, null, null, null);
		when(assembler.toMutationResponse(eq(tripId), eq(4L), any(Note.class))).thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpsertNoteCommand(
			tripId, actorId, 3L, PlanningScopeType.TRIP, null, "새 본문"
		));

		assertThat(result).isSameAs(stubResponse);
		verify(noteMapper).updateContent(eq(noteId), eq("새 본문"), eq(3L), any(Instant.class));
		verify(noteMapper, never()).insert(any(), any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("UPDATE 시 affectedRows가 0이면 PLANNING_VERSION_CONFLICT")
	void throwsOnVersionConflict() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();
		NoteRecord existing = new NoteRecord(noteId, tripId, PlanningScopeType.TRIP, null,
			"이전", 3L, null, Instant.now(), Instant.now());

		when(noteMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.of(existing));
		when(noteMapper.updateContent(eq(noteId), eq("new"), eq(2L), any()))
			.thenReturn(0);

		assertThatThrownBy(() -> handler.handle(new UpsertNoteCommand(
			tripId, actorId, 2L, PlanningScopeType.TRIP, null, "new"
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_VERSION_CONFLICT));
	}

	@Test
	@DisplayName("DAY scope인데 itineraryDayId가 null이면 PLANNING_SCOPE_DAY_MISMATCH")
	void dayScopeWithoutDayIdRejected() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();

		assertThatThrownBy(() -> handler.handle(new UpsertNoteCommand(
			tripId, actorId, 0L, PlanningScopeType.DAY, null, "본문"
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH));

		verify(noteMapper, never()).insert(any(), any(), any(), any(), any(), any());
		verify(noteMapper, never()).updateContent(any(), any(), anyLong(), any());
	}

	@Test
	@DisplayName("TRIP scope인데 itineraryDayId가 있으면 PLANNING_SCOPE_DAY_MISMATCH")
	void tripScopeWithDayIdRejected() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID dayId = UUID.randomUUID();

		assertThatThrownBy(() -> handler.handle(new UpsertNoteCommand(
			tripId, actorId, 0L, PlanningScopeType.TRIP, dayId, "본문"
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH));
	}
}
