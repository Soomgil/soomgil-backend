package com.soomgil.planning.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.query.ListChecklistsQuery;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMemberStatusMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListChecklistsQueryHandlerTest {

	private final ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
	private final ChecklistItemMapper itemMapper = mock(ChecklistItemMapper.class);
	private final ChecklistMemberStatusMapper statusMapper = mock(ChecklistMemberStatusMapper.class);
	private final PlanningAssembler assembler = mock(PlanningAssembler.class);
	private final TripMemberAccessChecker accessChecker = mock(TripMemberAccessChecker.class);

	private final ListChecklistsQueryHandler handler = new ListChecklistsQueryHandler(
		checklistMapper, itemMapper, statusMapper, assembler, accessChecker
	);

	@Test
	@DisplayName("scope/day 필터가 모두 null이면 trip의 모든 checklist를 items/statuses와 함께 조립한다")
	void listsAllChecklists() {
		UUID tripId = UUID.randomUUID();
		UUID viewerId = UUID.randomUUID();
		ChecklistRecord record1 = new ChecklistRecord(UUID.randomUUID(), tripId,
			PlanningScopeType.TRIP, null, "A", 1L, null, Instant.now(), Instant.now());
		ChecklistRecord record2 = new ChecklistRecord(UUID.randomUUID(), tripId,
			PlanningScopeType.TRIP, null, "B", 1L, null, Instant.now(), Instant.now());

		when(checklistMapper.findByTripIdWithFilters(tripId, null, null))
			.thenReturn(List.of(record1, record2));
		when(itemMapper.findByChecklistId(any())).thenReturn(List.of());
		when(statusMapper.findByItemId(any())).thenReturn(List.of());
		Checklist stub1 = new Checklist(record1.id(), tripId, PlanningScopeType.TRIP,
			null, "A", 1L, List.of());
		Checklist stub2 = new Checklist(record2.id(), tripId, PlanningScopeType.TRIP,
			null, "B", 1L, List.of());
		when(assembler.toChecklistDto(any(ChecklistRecord.class), any(), any()))
			.thenReturn(stub1, stub2);

		List<Checklist> result = handler.handle(new ListChecklistsQuery(
			tripId, null, null, viewerId
		));

		assertThat(result).containsExactly(stub1, stub2);
		verify(checklistMapper).findByTripIdWithFilters(tripId, null, null);
		verify(accessChecker).requireMember(tripId, viewerId);
	}

	@Test
	@DisplayName("scopeType 필터를 전달하면 mapper에 같은 값을 넘긴다")
	void passesScopeFilter() {
		UUID tripId = UUID.randomUUID();
		UUID dayId = UUID.randomUUID();

		when(checklistMapper.findByTripIdWithFilters(tripId, PlanningScopeType.DAY, dayId))
			.thenReturn(List.of());

		List<Checklist> result = handler.handle(new ListChecklistsQuery(
			tripId, PlanningScopeType.DAY, dayId, UUID.randomUUID()
		));

		assertThat(result).isEmpty();
		verify(checklistMapper).findByTripIdWithFilters(tripId, PlanningScopeType.DAY, dayId);
	}

	@Test
	@DisplayName("조회 결과가 비어있으면 빈 리스트를 반환한다")
	void emptyResultReturnsEmptyList() {
		UUID tripId = UUID.randomUUID();

		when(checklistMapper.findByTripIdWithFilters(tripId, null, null)).thenReturn(List.of());

		List<Checklist> result = handler.handle(new ListChecklistsQuery(
			tripId, null, null, UUID.randomUUID()
		));

		assertThat(result).isEmpty();
	}
}
