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
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.DeleteChecklistItemCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeleteChecklistItemCommandHandlerTest {

	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final DeleteChecklistItemCommandHandler handler = new DeleteChecklistItemCommandHandler(
		itemMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("활성 item을 baseVersion 일치로 soft delete한다")
	void deletesItem() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		ChecklistItemRecord existing = new ChecklistItemRecord(itemId, checklistId, 2,
			"본문", 3L, null, Instant.now(), Instant.now());

		when(itemMapper.findById(itemId)).thenReturn(Optional.of(existing));
		when(itemMapper.softDelete(eq(itemId), eq(3L), any())).thenReturn(1);
		ChecklistItem stubItem = new ChecklistItem(itemId, checklistId, 2, "본문",
			List.of(), null);
		when(assembler.toItemDto(any(), any())).thenReturn(stubItem);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 4L, null, false, false, null, null, stubItem, null);
		when(assembler.toMutationResponse(eq(tripId), eq(4L), any(ChecklistItem.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new DeleteChecklistItemCommand(
			tripId, checklistId, itemId, UUID.randomUUID(), 3L
		));

		assertThat(result).isSameAs(stubResponse);
		verify(itemMapper).softDelete(eq(itemId), eq(3L), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("식별자로 찾을 수 없으면 PLANNING_ITEM_NOT_FOUND")
	void notFoundThrows() {
		UUID itemId = UUID.randomUUID();

		when(itemMapper.findById(itemId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new DeleteChecklistItemCommand(
			UUID.randomUUID(), UUID.randomUUID(), itemId, UUID.randomUUID(), 1L
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_ITEM_NOT_FOUND));

		verify(itemMapper, never()).softDelete(any(), anyLong(), any());
	}
}
