package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.CreateMapDrawingCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateMapDrawingHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final CreateMapDrawingHandler handler = new CreateMapDrawingHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z"),
		new ObjectMapper()
	);

	@Test
	void createsMapDrawingAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new CreateMapDrawingCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			DrawingType.POLYGON,
			Map.of("type", "Polygon", "coordinates", List.of()),
			Map.of("fill", "#ffaa00"),
			"메모 영역",
			4
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.drawing().drawingType()).isEqualTo(DrawingType.POLYGON);
		assertThat(result.drawing().geometryFormat()).isEqualTo(GeometryFormat.GEOJSON);
		assertThat(repository.insertedDrawing.geometry()).contains("\"Polygon\"");
		assertThat(repository.insertedDrawing.sortOrder()).isEqualTo(4);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("CREATE_MAP_DRAWING");
		assertThat(eventRepository.lastEvent.aggregateId()).isEqualTo(repository.insertedDrawing.id());
	}

	@Test
	void rejectsMissingGeometry() {
		assertThatThrownBy(() -> handler.handle(new CreateMapDrawingCommand(
			TRIP_ID,
			USER_ID,
			0,
			null,
			DrawingType.LINE,
			Map.of(),
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsMissingDayScope() {
		repository.dayExists = false;

		assertThatThrownBy(() -> handler.handle(new CreateMapDrawingCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			DrawingType.LINE,
			Map.of("type", "LineString"),
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private boolean dayExists = true;
		private MapDrawingCreate insertedDrawing;

		@Override
		public OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt) {
			if (currentVersion != baseVersion) {
				return OptionalLong.empty();
			}
			currentVersion++;
			return OptionalLong.of(currentVersion);
		}

		@Override
		public OptionalLong findItineraryVersion(UUID tripId) {
			return OptionalLong.of(currentVersion);
		}

		@Override
		public void insertDay(ItineraryDayCreate day) {
		}

		@Override
		public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public void insertItem(ItineraryItemCreate item) {
		}

		@Override
		public void insertMapDrawing(MapDrawingCreate drawing) {
			this.insertedDrawing = drawing;
		}

		@Override
		public boolean existsDay(UUID tripId, UUID dayId) {
			return dayExists;
		}

		@Override
		public long countDays(UUID tripId) {
			return 1;
		}

		@Override
		public boolean existsItem(UUID tripId, UUID itemId) {
			return false;
		}

		@Override
		public long countActiveItems(UUID tripId) {
			return 0;
		}

		@Override
		public void updateDayOrder(ItineraryDayOrderUpdate update) {
		}

		@Override
		public void updateItemOrder(ItineraryItemOrderUpdate update) {
		}
	}

	private static class CapturingEventRepository implements CollaborationCommandEventRepository {

		private CollaborationCommandEvent lastEvent;

		@Override
		public void save(CollaborationCommandEvent event) {
			this.lastEvent = event;
		}
	}
}
