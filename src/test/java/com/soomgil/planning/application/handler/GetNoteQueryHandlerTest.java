package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.query.GetNoteQuery;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.NoteRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.infrastructure.persistence.mapper.NoteMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetNoteQueryHandlerTest {

	private final NoteMapper noteMapper = mock(NoteMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);

	private final GetNoteQueryHandler handler = new GetNoteQueryHandler(
		noteMapper, assembler, accessChecker
	);

	@Test
	@DisplayName("(trip, scope, day)로 활성 note를 찾으면 DTO로 조립해 반환한다")
	void returnsNoteDto() {
		UUID tripId = UUID.randomUUID();
		UUID viewerId = UUID.randomUUID();
		NoteRecord record = new NoteRecord(UUID.randomUUID(), tripId, PlanningScopeType.TRIP, null,
			"본문", viewerId, viewerId, null, null, Instant.now(), Instant.now());

		when(noteMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.of(record));
		Note stubNote = new Note(record.id(), tripId, PlanningScopeType.TRIP, null, "본문", null);
		when(assembler.toNoteDto(record)).thenReturn(stubNote);

		Note result = handler.handle(new GetNoteQuery(tripId, PlanningScopeType.TRIP, null, viewerId));

		assertThat(result).isSameAs(stubNote);
		verify(assembler).toNoteDto(record);
		verify(accessChecker).requireMember(tripId, viewerId);
	}

	@Test
	@DisplayName("note가 없으면 PLANNING_NOTE_NOT_FOUND")
	void notFoundThrows() {
		UUID tripId = UUID.randomUUID();

		when(noteMapper.findByTripScopeDay(tripId, PlanningScopeType.TRIP, null))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> handler.handle(new GetNoteQuery(
			tripId, PlanningScopeType.TRIP, null, UUID.randomUUID()
		)))
			.isInstanceOf(PlanningException.class)
			.satisfies(ex -> assertThat(((PlanningException) ex).errorCode())
				.isEqualTo(ErrorCode.PLANNING_NOTE_NOT_FOUND));
	}
}
