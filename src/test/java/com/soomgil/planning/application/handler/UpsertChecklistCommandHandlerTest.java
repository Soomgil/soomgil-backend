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
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpsertChecklistCommandHandlerTest {

	private final ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final UpsertChecklistCommandHandler handler = new UpsertChecklistCommandHandler(
		checklistMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("신규 checklist는 INSERT하고 version=1로 응답한다")
	void insertsNewChecklist() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();

		when(checklistMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.empty());
		Checklist stubChecklist = new Checklist(UUID.randomUUID(), tripId, PlanningScopeType.TRIP,
			null, "준비물", 1L, List.of());
		when(assembler.toChecklistDto(any(ChecklistRecord.class), any(), any()))
			.thenReturn(stubChecklist);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 1L, null, false, false, null, stubChecklist, null, null);
		when(assembler.toMutationResponse(eq(tripId), eq(1L), any(Checklist.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpsertChecklistCommand(
			tripId, actorId, 0L, PlanningScopeType.TRIP, null, "준비물"
		));

		assertThat(result).isSameAs(stubResponse);
		verify(checklistMapper).insert(any(UUID.class), eq(tripId), eq(PlanningScopeType.TRIP),
			eq(null), eq("준비물"), any(Instant.class));
		verify(checklistMapper, never()).updateTitle(any(), any(), anyLong(), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("기존 checklist가 있으면 title을 UPDATE한다")
	void updatesExistingChecklist() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		ChecklistRecord existing = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "옛 제목", 2L, null, Instant.now(), Instant.now());

		when(checklistMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.of(existing));
		when(checklistMapper.updateTitle(eq(checklistId), eq("새 제목"), eq(2L), any()))
			.thenReturn(1);
		Checklist stubChecklist = new Checklist(checklistId, tripId, PlanningScopeType.TRIP,
			null, "새 제목", 3L, List.of());
		when(assembler.toChecklistDto(any(ChecklistRecord.class), any(), any()))
			.thenReturn(stubChecklist);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 3L, null, false, false, null, stubChecklist, null, null);
		when(assembler.toMutationResponse(eq(tripId), eq(3L), any(Checklist.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpsertChecklistCommand(
			tripId, actorId, 2L, PlanningScopeType.TRIP, null, "새 제목"
		));

		assertThat(result).isSameAs(stubResponse);
		verify(checklistMapper).updateTitle(eq(checklistId), eq("새 제목"), eq(2L), any());
	}

	@Test
	@DisplayName("UPDATE 시 affectedRows가 0이면 PLANNING_VERSION_CONFLICT")
	void versionConflictThrows() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		ChecklistRecord existing = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "옛 제목", 2L, null, Instant.now(), Instant.now());

		when(checklistMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.of(existing));
		when(checklistMapper.updateTitle(eq(checklistId), eq("new"), eq(1L), any()))
			.thenReturn(0);

		assertThatThrownBy(() -> handler.handle(new UpsertChecklistCommand(
			tripId, UUID.randomUUID(), 1L, PlanningScopeType.TRIP, null, "new"
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_VERSION_CONFLICT));
	}

	@Test
	@DisplayName("DAY scope인데 itineraryDayId가 null이면 PLANNING_SCOPE_DAY_MISMATCH")
	void dayScopeWithoutDayIdRejected() {
		UUID tripId = UUID.randomUUID();

		assertThatThrownBy(() -> handler.handle(new UpsertChecklistCommand(
			tripId, UUID.randomUUID(), 0L, PlanningScopeType.DAY, null, "제목"
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_SCOPE_DAY_MISMATCH));

		verify(checklistMapper, never()).insert(any(), any(), any(), any(), any(), any());
	}
}
