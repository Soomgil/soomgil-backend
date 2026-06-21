package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.DeleteChecklistCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeleteChecklistCommandHandlerTest {

	private final ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final DeleteChecklistCommandHandler handler = new DeleteChecklistCommandHandler(
		checklistMapper, itemMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("checklist를 soft delete하면 연결된 item도 cascade로 soft delete된다")
	void deletesChecklistAndCascadesItems() {
		UUID tripId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		ChecklistRecord existing = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", actorId, actorId, null, null, Instant.now(), Instant.now());

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.of(existing));
		Checklist stubChecklist = new Checklist(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", List.of());
		when(assembler.toChecklistDto(any(ChecklistRecord.class), any(), any()))
			.thenReturn(stubChecklist);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, null, null, false, false, null, stubChecklist, null, null);
		when(assembler.toMutationResponse(eq(tripId), any(Checklist.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new DeleteChecklistCommand(
			tripId, checklistId, actorId
		));

		assertThat(result).isSameAs(stubResponse);
		verify(checklistMapper).softDelete(eq(checklistId), eq(actorId), any());
		verify(itemMapper).softDeleteByChecklistId(eq(checklistId), eq(actorId), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("식별자로 찾을 수 없으면 PLANNING_CHECKLIST_NOT_FOUND")
	void notFoundThrows() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new DeleteChecklistCommand(
			tripId, checklistId, UUID.randomUUID()
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_CHECKLIST_NOT_FOUND));

		verify(itemMapper, never()).softDeleteByChecklistId(any(), any(), any());
	}
}
