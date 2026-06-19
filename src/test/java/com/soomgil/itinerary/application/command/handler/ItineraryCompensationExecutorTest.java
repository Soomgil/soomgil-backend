package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.MapDrawingSnapshotUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ItineraryCompensationExecutorTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ITEM_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-06-18T00:00:00Z");

	private final ItineraryCommandRepository repository = mock(ItineraryCommandRepository.class);
	private final ItineraryCompensationExecutor executor = new ItineraryCompensationExecutor(repository, new ObjectMapper());

	@Test
	void softDeletesCreatedItemForUndo() {
		String payload = "{\"action\":\"DELETE_ITINERARY_ITEM\",\"itemId\":\"" + ITEM_ID + "\"}";
		when(repository.softDeleteItem(TRIP_ID, ITEM_ID, USER_ID, NOW)).thenReturn(true);

		assertThat(executor.supports(payload)).isTrue();
		executor.execute(TRIP_ID, USER_ID, payload, NOW);

		verify(repository).softDeleteItem(TRIP_ID, ITEM_ID, USER_ID, NOW);
	}

	@Test
	void rejectsChangedCompensationTarget() {
		String payload = "{\"action\":\"DELETE_ITINERARY_ITEM\",\"itemId\":\"" + ITEM_ID + "\"}";

		assertThatThrownBy(() -> executor.execute(TRIP_ID, USER_ID, payload, NOW))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT));
	}

	@Test
	void restoresDeletedItemForRedo() {
		String payload = "{\"action\":\"RESTORE_ITINERARY_ITEM\",\"itemId\":\"" + ITEM_ID + "\"}";
		when(repository.restoreItem(TRIP_ID, ITEM_ID, USER_ID, NOW)).thenReturn(true);

		assertThat(executor.supports(payload)).isTrue();
		executor.execute(TRIP_ID, USER_ID, payload, NOW);

		verify(repository).restoreItem(TRIP_ID, ITEM_ID, USER_ID, NOW);
	}

	@Test
	void recreatesDeletedDayForRedo() {
		String payload = "{\"action\":\"RESTORE_ITINERARY_DAY\",\"dayId\":\"" + ITEM_ID
			+ "\",\"groupType\":\"DAY\",\"dayNumber\":1,\"date\":\"2026-07-01\","
			+ "\"title\":\"첫째 날\",\"sortOrder\":2}";

		executor.execute(TRIP_ID, USER_ID, payload, NOW);

		org.mockito.ArgumentCaptor<ItineraryDayCreate> captor =
			org.mockito.ArgumentCaptor.forClass(ItineraryDayCreate.class);
		verify(repository).insertDay(captor.capture());
		assertThat(captor.getValue().id()).isEqualTo(ITEM_ID);
		assertThat(captor.getValue().title()).isEqualTo("첫째 날");
		assertThat(captor.getValue().sortOrder()).isEqualTo(2);
	}

	@Test
	void restoresNullableMapDrawingFieldsFromSnapshot() {
		String payload = "{\"action\":\"UPDATE_MAP_DRAWING\",\"drawingId\":\"" + ITEM_ID
			+ "\",\"geometry\":{\"type\":\"LineString\"},\"style\":null,\"label\":null,\"sortOrder\":0}";
		when(repository.applyMapDrawingSnapshot(org.mockito.ArgumentMatchers.any()))
			.thenReturn(java.util.Optional.of(new MapDrawingUpdateResult(
				ITEM_ID, null, DrawingType.LINE, GeometryFormat.GEOJSON,
				"{\"type\":\"LineString\"}", null, null, 0, 2L)));

		executor.execute(TRIP_ID, USER_ID, payload, NOW);

		org.mockito.ArgumentCaptor<MapDrawingSnapshotUpdate> captor =
			org.mockito.ArgumentCaptor.forClass(MapDrawingSnapshotUpdate.class);
		verify(repository).applyMapDrawingSnapshot(captor.capture());
		assertThat(captor.getValue().style()).isNull();
		assertThat(captor.getValue().label()).isNull();
	}
}
