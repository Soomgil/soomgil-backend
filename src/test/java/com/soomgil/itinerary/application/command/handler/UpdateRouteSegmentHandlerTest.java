package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateRouteSegmentCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryDayUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdate;
import com.soomgil.itinerary.application.port.RouteSegmentUpdateResult;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateRouteSegmentHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ROUTE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
	private static final UUID ORIGIN_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID DESTINATION_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final com.soomgil.itinerary.application.port.MapMatchingClient routingClient =
		org.mockito.Mockito.mock(com.soomgil.itinerary.application.port.MapMatchingClient.class);
	private final UpdateRouteSegmentHandler handler = new UpdateRouteSegmentHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z"),
		new ObjectMapper(),
		routingClient
	);

	@Test
	void updatesRouteSegmentAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new UpdateRouteSegmentCommand(
			TRIP_ID,
			USER_ID,
			0,
			ROUTE_ID,
			RouteMode.WALKING,
			Map.of("type", "LineString", "coordinates", List.of(List.of(127.0, 37.0), List.of(127.1, 37.1))),
			120.5,
			80.0,
			0.9
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.route().id()).isEqualTo(ROUTE_ID);
		assertThat(result.route().mode()).isEqualTo(RouteMode.WALKING);
		assertThat(result.route().providerProfile()).isEqualTo("mapbox/walking");
		assertThat(result.affectedRouteIds()).containsExactly(ROUTE_ID);
		assertThat(repository.lastUpdate.providerProfile()).isEqualTo("mapbox/walking");
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("UPDATE_ROUTE_SEGMENT");
		assertThat(eventRepository.lastEvent.inversePayload()).contains("UPDATE_ROUTE_SEGMENT", "DRIVING");
		assertThat(eventRepository.lastEvent.redoPayload()).contains("UPDATE_ROUTE_SEGMENT", "WALKING");
	}

	@Test
	void rejectsMissingUpdateFields() {
		assertThatThrownBy(() -> handler.handle(new UpdateRouteSegmentCommand(
			TRIP_ID,
			USER_ID,
			0,
			ROUTE_ID,
			null,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(RouteMode.class)
	void modeOnlyUpdateRecalculatesGeometryAndRecordsUndo(RouteMode mode) {
		Map<String, Object> geometry = Map.of("type", "LineString", "coordinates",
			List.of(List.of(127.0, 37.0), List.of(127.05, 37.03), List.of(127.1, 37.1)));
		org.mockito.Mockito.when(routingClient.match(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			var request = (com.soomgil.itinerary.application.port.MapMatchClientRequest) invocation.getArgument(0);
			assertThat(request.providerProfile()).isEqualTo("mapbox/" + mode.name().toLowerCase(java.util.Locale.ROOT));
			assertThat(request.coordinates()).hasSize(2);
			return new com.soomgil.itinerary.application.port.MapMatchClientResult(geometry, List.of(), Map.of(), 500.0, 150.0, null);
		});
		var result = handler.handle(new UpdateRouteSegmentCommand(TRIP_ID, USER_ID, 0, ROUTE_ID, mode, null, null, null, null));
		assertThat(result.route().geometry()).isEqualTo(geometry);
		assertThat(result.route().distanceMeters()).isEqualTo(500.0);
		assertThat(result.route().durationSeconds()).isEqualTo(150.0);
		assertThat(repository.lastUpdate.provider()).isEqualTo("MAPBOX");
		assertThat(eventRepository.lastEvent.inversePayload()).contains("DRIVING", "100.0");
		assertThat(eventRepository.lastEvent.redoPayload()).contains(mode.name(), "500.0");
	}

	@Test
	void failedRecalculationPreservesRouteAndVersion() {
		var original = repository.current;
		org.mockito.Mockito.when(routingClient.match(org.mockito.ArgumentMatchers.any()))
			.thenThrow(new com.soomgil.itinerary.application.port.MapMatchingException("NoRoute", "Unavailable"));
		assertThatThrownBy(() -> handler.handle(new UpdateRouteSegmentCommand(TRIP_ID, USER_ID, 0, ROUTE_ID, RouteMode.CYCLING, null, null, null, null)))
			.isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.ROUTE_CALCULATION_FAILED));
		assertThat(repository.current).isEqualTo(original);
		assertThat(repository.currentVersion).isZero();
		assertThat(eventRepository.lastEvent).isNull();
	}

	@Test
	void rejectsMissingRoute() {
		repository.existsRoute = false;

		assertThatThrownBy(() -> handler.handle(new UpdateRouteSegmentCommand(
			TRIP_ID,
			USER_ID,
			0,
			ROUTE_ID,
			RouteMode.DRIVING,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private boolean existsRoute = true;
		private RouteSegmentUpdate lastUpdate;
		private RouteSegmentUpdateResult current = new RouteSegmentUpdateResult(
			ROUTE_ID, ORIGIN_ITEM_ID, DESTINATION_ITEM_ID, RouteMode.DRIVING, "MAPBOX", "mapbox/driving",
			GeometryFormat.GEOJSON, "{\"type\":\"LineString\",\"coordinates\":[[127.0,37.0],[127.1,37.1]]}", 100.0, 60.0, 0.8);

		@Override
		public Optional<RouteSegmentUpdateResult> findRouteSegment(UUID tripId, UUID routeId) {
			return existsRoute ? Optional.of(current) : Optional.empty();
		}

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
		public Optional<ItineraryDayReadModel> findDay(UUID tripId, UUID dayId) {
			return Optional.empty();
		}

		@Override
		public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public Optional<ItineraryDayReadModel> updateDay(ItineraryDayUpdate update) {
			return Optional.empty();
		}

		@Override
		public long countActiveItemsByDay(UUID tripId, UUID dayId) {
			return 0;
		}

		@Override
		public boolean deleteDay(UUID tripId, UUID dayId) {
			return false;
		}

		@Override
		public void insertItem(ItineraryItemCreate item) {
		}

		@Override
		public Optional<ItineraryItemReadModel> findItem(UUID tripId, UUID itemId) {
			return Optional.empty();
		}

		@Override
		public Optional<ItineraryItemReadModel> updateItem(ItineraryItemUpdate update) {
			return Optional.empty();
		}

		@Override
		public void insertMapDrawing(MapDrawingCreate drawing) {
		}

		@Override
		public void insertRouteSegment(RouteSegmentCreate route) {
		}

		@Override
		public Optional<RouteSegmentUpdateResult> updateRouteSegment(RouteSegmentUpdate update) {
			this.lastUpdate = update;
			current = new RouteSegmentUpdateResult(
				update.routeId(),
				ORIGIN_ITEM_ID,
				DESTINATION_ITEM_ID,
				update.mode() == null ? RouteMode.DRIVING : update.mode(),
				"MAPBOX",
				update.providerProfile() == null ? "mapbox/driving" : update.providerProfile(),
				GeometryFormat.GEOJSON,
				update.geometry() == null ? "{\"type\":\"LineString\"}" : update.geometry(),
				update.distanceMeters(),
				update.durationSeconds(),
				update.confidence()
			);
			return Optional.of(current);
		}

		@Override
		public Long insertRouteMatchRequest(RouteMatchRequestLog request) {
			return 1L;
		}

		@Override
		public boolean existsActiveRouteSegment(UUID tripId, UUID routeId) {
			return existsRoute;
		}

		@Override
		public boolean softDeleteRouteSegment(UUID tripId, UUID routeId, UUID deletedByUserId, Instant deletedAt) {
			return false;
		}

		@Override
		public boolean existsActiveMapDrawing(UUID tripId, UUID drawingId) {
			return false;
		}

		@Override
		public boolean softDeleteMapDrawing(UUID tripId, UUID drawingId, UUID deletedByUserId, Instant deletedAt) {
			return false;
		}

		@Override
		public Optional<MapDrawingUpdateResult> updateMapDrawing(MapDrawingUpdate update) {
			return Optional.empty();
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
		public List<UUID> findActiveRouteIdsByItem(UUID tripId, UUID itemId) {
			return List.of();
		}

		@Override
		public boolean softDeleteItem(UUID tripId, UUID itemId, UUID deletedByUserId, Instant deletedAt) {
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
