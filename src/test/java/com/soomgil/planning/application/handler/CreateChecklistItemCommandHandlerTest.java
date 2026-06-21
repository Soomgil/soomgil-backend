package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
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

class CreateChecklistItemCommandHandlerTest {

	private final ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final CreateChecklistItemCommandHandler handler = new CreateChecklistItemCommandHandler(
		checklistMapper, itemMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("sortOrder를 명시하지 않으면 (max + 1)로 자동 설정한다")
	void autoSortOrderWhenNull() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistRecord checklist = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", actorId, actorId, null, null, Instant.now(), Instant.now());

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.of(checklist));
		when(itemMapper.findMaxSortOrder(checklistId)).thenReturn(5);
		ChecklistItem stubItem = new ChecklistItem(UUID.randomUUID(), checklistId,
			6, "새 항목", List.of(), null);
		when(assembler.toItemDto(any(), any())).thenReturn(stubItem);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, stubItem, null);
		when(assembler.toMutationResponse(eq(tripId), any(ChecklistItem.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new CreateChecklistItemCommand(
			tripId, checklistId, actorId, "새 항목", null
		));

		assertThat(result).isSameAs(stubResponse);
		verify(itemMapper).insert(any(UUID.class), eq(checklistId), eq(6), eq("새 항목"),
			eq(actorId), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("sortOrder를 명시하면 그 값을 그대로 사용한다")
	void usesExplicitSortOrder() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistRecord checklist = new ChecklistRecord(checklistId, tripId, PlanningScopeType.TRIP,
			null, "제목", actorId, actorId, null, null, Instant.now(), Instant.now());

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.of(checklist));
		ChecklistItem stubItem = new ChecklistItem(UUID.randomUUID(), checklistId,
			10, "새 항목", List.of(), null);
		when(assembler.toItemDto(any(), any())).thenReturn(stubItem);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, stubItem, null);
		when(assembler.toMutationResponse(eq(tripId), any(ChecklistItem.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new CreateChecklistItemCommand(
			tripId, checklistId, actorId, "새 항목", 10
		));

		assertThat(result).isSameAs(stubResponse);
		verify(itemMapper).insert(any(UUID.class), eq(checklistId), eq(10), eq("새 항목"),
			eq(actorId), any());
		verify(itemMapper, never()).findMaxSortOrder(any());
	}

	@Test
	@DisplayName("checklist가 없거나 삭제됐으면 PLANNING_CHECKLIST_NOT_FOUND")
	void missingChecklistThrows() {
		UUID checklistId = UUID.randomUUID();

		when(checklistMapper.findById(checklistId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new CreateChecklistItemCommand(
			UUID.randomUUID(), checklistId, UUID.randomUUID(), "본문", null
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_CHECKLIST_NOT_FOUND));

		verify(itemMapper, never()).insert(any(), any(), anyInt(), any(), any(), any());
	}
}
