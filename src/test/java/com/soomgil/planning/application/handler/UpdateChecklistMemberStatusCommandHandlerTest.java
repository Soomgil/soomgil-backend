package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.ChecklistMemberStatus;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.application.command.UpdateChecklistMemberStatusCommand;
import com.soomgil.planning.application.event.PlanningRealtimeEvent;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.PlanningEventBroadcaster;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.ChecklistMemberStatusRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMemberStatusMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateChecklistMemberStatusCommandHandlerTest {

	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final ChecklistMemberStatusMapper statusMapper = mock(ChecklistMemberStatusMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);
	private final PlanningEventBroadcaster broadcaster = mock(PlanningEventBroadcaster.class);

	private final UpdateChecklistMemberStatusCommandHandler handler =
		new UpdateChecklistMemberStatusCommandHandler(
			itemMapper, statusMapper, assembler, accessChecker, broadcaster
		);

	@Test
	@DisplayName("first touch: 기존 멤버 상태가 없으면 version=1로 INSERT한다")
	void firstTouchInserts() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistItemRecord item = new ChecklistItemRecord(itemId, checklistId, 0, "본문",
			1L, null, Instant.now(), Instant.now());

		when(itemMapper.findById(itemId)).thenReturn(Optional.of(item));
		when(statusMapper.findByItemIdAndUserId(itemId, actorId)).thenReturn(Optional.empty());
		ChecklistMemberStatus stubStatus = new ChecklistMemberStatus(null, true, null, null);
		when(assembler.toMemberStatusDto(any(ChecklistMemberStatusRecord.class))).thenReturn(stubStatus);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 1L, null, false, false, null, null, null, stubStatus);
		when(assembler.toMutationResponse(eq(tripId), eq(1L), any(ChecklistMemberStatus.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpdateChecklistMemberStatusCommand(
			tripId, checklistId, itemId, actorId, 0L, true
		));

		assertThat(result).isSameAs(stubResponse);
		verify(statusMapper).insert(eq(itemId), eq(actorId), eq(true), any(), any());
		verify(statusMapper, never()).updateStatus(any(), any(), anyBoolean(), any(), anyLong(), any());
		verify(broadcaster).broadcast(any(PlanningRealtimeEvent.class));
	}

	@Test
	@DisplayName("기존 멤버 상태가 있으면 baseVersion 검증 후 UPDATE한다")
	void existingStatusUpdates() {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistItemRecord item = new ChecklistItemRecord(itemId, checklistId, 0, "본문",
			1L, null, Instant.now(), Instant.now());
		ChecklistMemberStatusRecord existing = new ChecklistMemberStatusRecord(itemId, actorId,
			false, null, 2L, Instant.now());

		when(itemMapper.findById(itemId)).thenReturn(Optional.of(item));
		when(statusMapper.findByItemIdAndUserId(itemId, actorId)).thenReturn(Optional.of(existing));
		when(statusMapper.updateStatus(eq(itemId), eq(actorId), eq(true), any(), eq(2L), any()))
			.thenReturn(1);
		ChecklistMemberStatus stubStatus = new ChecklistMemberStatus(null, true, null, null);
		when(assembler.toMemberStatusDto(any(ChecklistMemberStatusRecord.class))).thenReturn(stubStatus);
		PlanningMutationResponse stubResponse = new PlanningMutationResponse(
			tripId, 3L, null, false, false, null, null, null, stubStatus);
		when(assembler.toMutationResponse(eq(tripId), eq(3L), any(ChecklistMemberStatus.class)))
			.thenReturn(stubResponse);

		PlanningMutationResponse result = handler.handle(new UpdateChecklistMemberStatusCommand(
			tripId, checklistId, itemId, actorId, 2L, true
		));

		assertThat(result).isSameAs(stubResponse);
		verify(statusMapper).updateStatus(eq(itemId), eq(actorId), eq(true), any(), eq(2L), any());
		verify(statusMapper, never()).insert(any(), any(), anyBoolean(), any(), any());
	}

	@Test
	@DisplayName("UPDATE 시 affectedRows가 0이면 PLANNING_VERSION_CONFLICT")
	void versionConflictThrows() {
		UUID itemId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		ChecklistItemRecord item = new ChecklistItemRecord(itemId, UUID.randomUUID(), 0, "본문",
			1L, null, Instant.now(), Instant.now());
		ChecklistMemberStatusRecord existing = new ChecklistMemberStatusRecord(itemId, actorId,
			false, null, 2L, Instant.now());

		when(itemMapper.findById(itemId)).thenReturn(Optional.of(item));
		when(statusMapper.findByItemIdAndUserId(itemId, actorId)).thenReturn(Optional.of(existing));
		when(statusMapper.updateStatus(eq(itemId), eq(actorId), anyBoolean(), any(), eq(1L), any()))
			.thenReturn(0);

		assertThatThrownBy(() -> handler.handle(new UpdateChecklistMemberStatusCommand(
			UUID.randomUUID(), UUID.randomUUID(), itemId, actorId, 1L, true
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_VERSION_CONFLICT));
	}
}
