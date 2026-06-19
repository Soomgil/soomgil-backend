package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.ChecklistItemOrder;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.ReorderChecklistItemsCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
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

class ReorderChecklistItemsCommandHandlerTest {

	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final ReorderChecklistItemsCommandHandler handler = new ReorderChecklistItemsCommandHandler(
		itemMapper, checklistMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("모든 item이 checklist 소속이면 per-item updateSortOrder가 순차 호출된다")
	void reordersAllItems() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID item1 = UUID.randomUUID();
		UUID item2 = UUID.randomUUID();
		ChecklistRecord checklist = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", 2L, null, Instant.now(), Instant.now());

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.of(checklist));
		when(itemMapper.findById(item1)).thenReturn(Optional.of(
			new ChecklistItemRecord(item1, checklistId, 0, "a", 1L, null, Instant.now(), Instant.now())));
		when(itemMapper.findById(item2)).thenReturn(Optional.of(
			new ChecklistItemRecord(item2, checklistId, 1, "b", 1L, null, Instant.now(), Instant.now())));
		when(itemMapper.updateSortOrder(eq(item1), eq(0), eq(1L), any())).thenReturn(1);
		when(itemMapper.updateSortOrder(eq(item2), eq(1), eq(1L), any())).thenReturn(1);
		when(itemMapper.findByChecklistId(checklistId)).thenReturn(List.of());
		Checklist stubChecklist = new Checklist(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", 2L, List.of());
		when(assembler.toChecklistDto(any(), any(), any())).thenReturn(stubChecklist);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 2L, null, false, false, null, stubChecklist, null, null);
		when(assembler.toMutationResponse(eq(tripId), eq(2L), any(Checklist.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new ReorderChecklistItemsCommand(
			tripId, checklistId, UUID.randomUUID(), 2L,
			List.of(new ChecklistItemOrder(item1, 0), new ChecklistItemOrder(item2, 1))
		));

		assertThat(result).isSameAs(stubResponse);
		verify(itemMapper).updateSortOrder(eq(item1), eq(0), eq(1L), any());
		verify(itemMapper).updateSortOrder(eq(item2), eq(1), eq(1L), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("item이 다른 checklist에 속하면 PLANNING_ITEM_NOT_FOUND")
	void foreignItemRejected() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID otherChecklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		ChecklistRecord checklist = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", 2L, null, Instant.now(), Instant.now());

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.of(checklist));
		when(itemMapper.findById(itemId)).thenReturn(Optional.of(
			new ChecklistItemRecord(itemId, otherChecklistId, 0, "x", 1L, null,
				Instant.now(), Instant.now())));

		assertThatThrownBy(() -> handler.handle(new ReorderChecklistItemsCommand(
			tripId, checklistId, UUID.randomUUID(), 2L, List.of(new ChecklistItemOrder(itemId, 0))
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_ITEM_NOT_FOUND));

		verify(itemMapper, never()).updateSortOrder(any(), anyInt(), anyLong(), any());
	}

	@Test
	@DisplayName("per-item updateSortOrder가 하나라도 0을 반환하면 PLANNING_VERSION_CONFLICT")
	void versionConflictAborts() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID item1 = UUID.randomUUID();
		UUID item2 = UUID.randomUUID();
		ChecklistRecord checklist = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", 2L, null, Instant.now(), Instant.now());

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.of(checklist));
		when(itemMapper.findById(item1)).thenReturn(Optional.of(
			new ChecklistItemRecord(item1, checklistId, 0, "a", 1L, null, Instant.now(), Instant.now())));
		when(itemMapper.findById(item2)).thenReturn(Optional.of(
			new ChecklistItemRecord(item2, checklistId, 1, "b", 1L, null, Instant.now(), Instant.now())));
		when(itemMapper.updateSortOrder(eq(item1), eq(0), eq(1L), any())).thenReturn(1);
		when(itemMapper.updateSortOrder(eq(item2), eq(1), eq(1L), any())).thenReturn(0);

		assertThatThrownBy(() -> handler.handle(new ReorderChecklistItemsCommand(
			tripId, checklistId, UUID.randomUUID(), 2L,
			List.of(new ChecklistItemOrder(item1, 0), new ChecklistItemOrder(item2, 1))
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_VERSION_CONFLICT));
	}
}
