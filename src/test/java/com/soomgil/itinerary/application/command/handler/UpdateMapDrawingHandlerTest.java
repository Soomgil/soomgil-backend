package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateMapDrawingCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.DrawingType;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateMapDrawingHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DRAWING_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final UpdateMapDrawingHandler handler = new UpdateMapDrawingHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z"),
		new ObjectMapper()
	);

	@Test
	void updatesMapDrawingAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new UpdateMapDrawingCommand(
			TRIP_ID,
			USER_ID,
			0,
			DRAWING_ID,
			Map.of("type", "LineString"),
			Map.of("color", "#222222"),
			"수정된 선",
			3,
			0L
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.drawing().id()).isEqualTo(DRAWING_ID);
		assertThat(result.drawing().version()).isEqualTo(1);
		assertThat(result.drawing().label()).isEqualTo("수정된 선");
		assertThat(repository.lastUpdate.expectedVersion()).isEqualTo(0L);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("UPDATE_MAP_DRAWING");
	}

	@Test
	void rejectsMissingUpdateFields() {
		assertThatThrownBy(() -> handler.handle(new UpdateMapDrawingCommand(
			TRIP_ID,
			USER_ID,
			0,
			DRAWING_ID,
			null,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsDrawingVersionConflict() {
		repository.updateSucceeds = false;

		assertThatThrownBy(() -> handler.handle(new UpdateMapDrawingCommand(
			TRIP_ID,
			USER_ID,
			0,
			DRAWING_ID,
			Map.of("type", "LineString"),
			null,
			null,
			null,
			99L
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private boolean updateSucceeds = true;
		private MapDrawingUpdate lastUpdate;

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
		}

		@Override
		public void insertRouteSegment(RouteSegmentCreate route) {
		}

		@Override
		public Long insertRouteMatchRequest(RouteMatchRequestLog request) {
			return 1L;
		}

		@Override
		public boolean existsActiveRouteSegment(UUID tripId, UUID routeId) {
			return false;
		}

		@Override
		public boolean softDeleteRouteSegment(UUID tripId, UUID routeId, UUID deletedByUserId, Instant deletedAt) {
			return false;
		}

		@Override
		public boolean existsActiveMapDrawing(UUID tripId, UUID drawingId) {
			return true;
		}

		@Override
		public boolean softDeleteMapDrawing(UUID tripId, UUID drawingId, UUID deletedByUserId, Instant deletedAt) {
			return false;
		}

		@Override
		public Optional<MapDrawingUpdateResult> updateMapDrawing(MapDrawingUpdate update) {
			this.lastUpdate = update;
			if (!updateSucceeds) {
				return Optional.empty();
			}
			return Optional.of(new MapDrawingUpdateResult(
				update.drawingId(),
				null,
				DrawingType.LINE,
				GeometryFormat.GEOJSON,
				update.geometry(),
				update.style(),
				update.label(),
				update.sortOrder(),
				1L
			));
		}

		@Override
		public boolean existsDay(UUID tripId, UUID dayId) {
			return false;
		}

		@Override
		public long countDays(UUID tripId) {
			return 0;
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
