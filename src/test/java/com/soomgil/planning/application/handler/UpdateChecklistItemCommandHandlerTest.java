package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpdateChecklistItemCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateChecklistItemCommandHandlerTest {

	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final UpdateChecklistItemCommandHandler handler = new UpdateChecklistItemCommandHandler(
		itemMapper, assembler, accessChecker, broadcaster
	);

	@Test
	@DisplayName("content만 바꾸면 sortOrder=null과 함께 update가 호출된다 (COALESCE로 기존값 유지)")
	void updatesContentOnly() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistItemRecord existing = new ChecklistItemRecord(itemId, checklistId, 3,
			"이전 본문", actorId, actorId, null, null, Instant.now(), Instant.now());

		when(itemMapper.findById(itemId)).thenReturn(Optional.of(existing));
		ChecklistItem stubItem = new ChecklistItem(itemId, checklistId, 3, "새 본문",
			List.of(), null);
		when(assembler.toItemDto(any(), any())).thenReturn(stubItem);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, stubItem, null);
		when(assembler.toMutationResponse(eq(tripId), any(ChecklistItem.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpdateChecklistItemCommand(
			tripId, checklistId, itemId, actorId, "새 본문", null
		));

		assertThat(result).isSameAs(stubResponse);
		verify(itemMapper).update(eq(itemId), eq("새 본문"), eq(null), eq(actorId), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("sortOrder만 바꾸면 content=null과 함께 update가 호출된다")
	void updatesSortOrderOnly() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistItemRecord existing = new ChecklistItemRecord(itemId, checklistId, 3,
			"본문", actorId, actorId, null, null, Instant.now(), Instant.now());

		when(itemMapper.findById(itemId)).thenReturn(Optional.of(existing));
		ChecklistItem stubItem = new ChecklistItem(itemId, checklistId, 10, "본문",
			List.of(), null);
		when(assembler.toItemDto(any(), any())).thenReturn(stubItem);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, stubItem, null);
		when(assembler.toMutationResponse(eq(tripId), any(ChecklistItem.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpdateChecklistItemCommand(
			tripId, checklistId, itemId, actorId, null, 10
		));

		assertThat(result).isSameAs(stubResponse);
		verify(itemMapper).update(eq(itemId), eq(null), eq(10), eq(actorId), any());
	}
}
