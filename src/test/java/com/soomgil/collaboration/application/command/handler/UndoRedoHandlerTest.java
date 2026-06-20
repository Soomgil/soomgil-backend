package com.soomgil.collaboration.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.command.dto.UndoRedoAction;
import com.soomgil.collaboration.application.command.dto.UndoRedoCommand;
import com.soomgil.collaboration.application.command.dto.UndoRedoResult;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventReadModel;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.application.port.CollaborationCompensationExecutor;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UndoRedoHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID AGGREGATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final String SESSION_ID = "session-1";

	private final CollaborationCommandEventRepository eventRepository = mock(CollaborationCommandEventRepository.class);
	private final ItineraryCommandRepository itineraryRepository = mock(ItineraryCommandRepository.class);
	private final CollaborationCompensationExecutor compensationExecutor = mock(CollaborationCompensationExecutor.class);
	private final UndoRedoHandler handler = new UndoRedoHandler(
		eventRepository,
		itineraryRepository,
		new TripAccessGuard(tripRepository()),
		() -> Instant.parse("2026-06-18T00:00:00Z"),
		new ObjectMapper(),
		java.util.List.of(compensationExecutor)
	);

	@Test
	void recordsUndoEventAndReturnsAvailability() {
		when(eventRepository.findUndoCandidate(TRIP_ID, USER_ID, SESSION_ID, null))
			.thenReturn(Optional.of(candidate(
				1L,
				"CREATE_ITINERARY_DAY",
				"{\"dayId\":\"" + AGGREGATE_ID + "\"}",
				"{\"action\":\"DELETE_ITINERARY_DAY\",\"dayId\":\"" + AGGREGATE_ID + "\"}",
				null
			)));
		when(itineraryRepository.incrementItineraryVersion(TRIP_ID, 10L, Instant.parse("2026-06-18T00:00:00Z")))
			.thenReturn(OptionalLong.of(11L));
		when(eventRepository.saveReturningId(any())).thenReturn(2L);
		when(eventRepository.hasUndoCandidate(TRIP_ID, USER_ID, SESSION_ID)).thenReturn(false);
		when(eventRepository.hasRedoCandidate(TRIP_ID, USER_ID, SESSION_ID)).thenReturn(true);
		when(compensationExecutor.supports(any())).thenReturn(true);

		UndoRedoResult result = handler.handle(new UndoRedoCommand(TRIP_ID, USER_ID, SESSION_ID, 10L, null, UndoRedoAction.UNDO));

		assertThat(result.itineraryVersion()).isEqualTo(11L);
		assertThat(result.commandEventId()).isEqualTo(2L);
		assertThat(result.undoAvailable()).isFalse();
		assertThat(result.redoAvailable()).isTrue();
		verify(compensationExecutor).execute(
			org.mockito.ArgumentMatchers.eq(TRIP_ID),
			org.mockito.ArgumentMatchers.eq(USER_ID),
			org.mockito.ArgumentMatchers.contains("DELETE_ITINERARY_DAY"),
			org.mockito.ArgumentMatchers.eq(Instant.parse("2026-06-18T00:00:00Z"))
		);
		verify(eventRepository).saveReturningId(org.mockito.ArgumentMatchers.argThat(event ->
			event.source().equals("UNDO")
				&& event.commandType().equals("UNDO_CREATE_ITINERARY_DAY")
				&& event.payload().contains("\"targetEventId\":1")
		));
	}

	@Test
	void recordsRedoEventAndReturnsAvailability() {
		when(eventRepository.findRedoCandidate(TRIP_ID, USER_ID, SESSION_ID, 3L))
			.thenReturn(Optional.of(candidate(
				3L,
				"UNDO_CREATE_ITINERARY_DAY",
				"{\"action\":\"UNDO\",\"targetEventId\":1,\"command\":{\"action\":\"DELETE_ITINERARY_DAY\"}}",
				"{\"dayId\":\"" + AGGREGATE_ID + "\"}",
				"{\"action\":\"CREATE_ITINERARY_DAY\",\"dayId\":\"" + AGGREGATE_ID + "\"}"
			)));
		when(itineraryRepository.incrementItineraryVersion(TRIP_ID, 11L, Instant.parse("2026-06-18T00:00:00Z")))
			.thenReturn(OptionalLong.of(12L));
		when(eventRepository.saveReturningId(any())).thenReturn(4L);
		when(compensationExecutor.supports(any())).thenReturn(true);

		UndoRedoResult result = handler.handle(new UndoRedoCommand(TRIP_ID, USER_ID, SESSION_ID, 11L, 3L, UndoRedoAction.REDO));

		assertThat(result.itineraryVersion()).isEqualTo(12L);
		assertThat(result.commandEventId()).isEqualTo(4L);
		verify(eventRepository).saveReturningId(org.mockito.ArgumentMatchers.argThat(event ->
			event.source().equals("REDO")
				&& event.commandType().equals("REDO_CREATE_ITINERARY_DAY")
				&& event.payload().contains("\"targetUndoEventId\":3")
		));
	}

	@Test
	void rejectsWhenUndoCandidateIsMissing() {
		when(eventRepository.findUndoCandidate(TRIP_ID, USER_ID, SESSION_ID, null)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new UndoRedoCommand(TRIP_ID, USER_ID, SESSION_ID, 10L, null, UndoRedoAction.UNDO)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
			);
	}

	@Test
	void rejectsMissingWebsocketSessionId() {
		assertThatThrownBy(() -> handler.handle(new UndoRedoCommand(TRIP_ID, USER_ID, null, 10L, null, UndoRedoAction.UNDO)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	private CollaborationCommandEventReadModel candidate(
		Long id,
		String commandType,
		String payload,
		String inversePayload,
		String redoPayload
	) {
		return new CollaborationCommandEventReadModel(
			id,
			TRIP_ID,
			USER_ID,
			SESSION_ID,
			"USER",
			commandType,
			"ITINERARY_DAY",
			AGGREGATE_ID,
			9L,
			10L,
			payload,
			inversePayload,
			redoPayload,
			Instant.parse("2026-06-18T00:00:00Z")
		);
	}

	private TripQueryRepository tripRepository() {
		TripQueryRepository repository = mock(TripQueryRepository.class);
		when(repository.findTripAccess(TRIP_ID, USER_ID)).thenReturn(Optional.of(new TripAccessSnapshot(
			TRIP_ID,
			USER_ID,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			USER_ID
		)));
		return repository;
	}
}
