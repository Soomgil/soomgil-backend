package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.itinerary.application.command.dto.MapMatchRouteCommand;
import com.soomgil.itinerary.application.command.dto.MapMatchRouteResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.MapMatchClientRequest;
import com.soomgil.itinerary.application.port.MapMatchClientResult;
import com.soomgil.itinerary.application.port.MapMatchingClient;
import com.soomgil.itinerary.application.port.MapMatchingException;
import com.soomgil.itinerary.application.port.RouteCoordinate;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MapMatchRouteHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ORIGIN_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID DESTINATION_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();

	@Test
	void matchesRouteAndStoresRequestLog() {
		MapMatchRouteHandler handler = handler(request -> new MapMatchClientResult(
			Map.of("type", "LineString", "coordinates", List.of(List.of(127.0, 37.0), List.of(127.1, 37.1))),
			List.of(Map.of("waypoint_index", 0)),
			Map.of("code", "Ok"),
			120.0,
			60.0,
			0.98
		));

		MapMatchRouteResult result = handler.handle(command());

		assertThat(result.matchRequestId()).isEqualTo(11L);
		assertThat(result.mutation().route().providerProfile()).isEqualTo("mapbox/walking");
		assertThat(repository.insertedRoute).isNotNull();
		assertThat(repository.insertedLog.status()).isEqualTo("SUCCEEDED");
		assertThat(repository.insertedLog.tripRouteId()).isEqualTo(repository.insertedRoute.id());
	}

	@Test
	void defaultsMissingTidyToFalseWhenStoringRequestLog() {
		MapMatchRouteHandler handler = handler(request -> {
			assertThat(request.tidy()).isFalse();
			return new MapMatchClientResult(
				Map.of("type", "LineString", "coordinates", List.of(List.of(127.0, 37.0), List.of(127.1, 37.1))),
				List.of(Map.of("waypoint_index", 0)),
				Map.of("code", "Ok"),
				120.0,
				60.0,
				0.98
			);
		});

		MapMatchRouteResult result = handler.handle(command(null));

		assertThat(result.matchRequestId()).isEqualTo(11L);
		assertThat(repository.insertedLog.tidy()).isFalse();
	}

	@Test
	void fallsBackToRawTraceWhenProviderCannotMatch() {
		MapMatchRouteHandler handler = handler(request -> {
			throw new MapMatchingException("NoMatch", "No matching route found.");
		});

		MapMatchRouteResult result = handler.handle(command());

		assertThat(result.matchRequestId()).isEqualTo(11L);
		assertThat(result.mutation().route().provider()).isEqualTo("USER_TRACE");
		assertThat(result.mutation().route().providerProfile()).isEqualTo("user-trace/walking");
		assertThat(result.mutation().route().geometry()).containsEntry("type", "LineString");
		assertThat(repository.insertedRoute).isNotNull();
		assertThat(repository.insertedLog.status()).isEqualTo("FAILED");
		assertThat(repository.insertedLog.tripRouteId()).isEqualTo(repository.insertedRoute.id());
		assertThat(repository.insertedLog.errorCode()).isEqualTo("NoMatch");
		assertThat(result.matchingsMetadata()).containsEntry("code", "FALLBACK");
	}

	private MapMatchRouteHandler handler(MapMatchingClient client) {
		return new MapMatchRouteHandler(
			repository,
			new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
			client,
			new SaveRouteSegmentHandler(
				repository,
				eventRepository,
				new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
				() -> Instant.parse("2026-06-17T00:00:00Z"),
				new ObjectMapper()
			),
			() -> Instant.parse("2026-06-17T00:00:00Z"),
			new ObjectMapper()
		);
	}

	private MapMatchRouteCommand command() {
		return command(true);
	}

	private MapMatchRouteCommand command(Boolean tidy) {
		return new MapMatchRouteCommand(
			TRIP_ID,
			USER_ID,
			0,
			ORIGIN_ITEM_ID,
			DESTINATION_ITEM_ID,
			RouteMode.WALKING,
			List.of(new RouteCoordinate(127.0, 37.0), new RouteCoordinate(127.1, 37.1)),
			null,
			tidy
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private final Set<UUID> itemIds = Set.of(ORIGIN_ITEM_ID, DESTINATION_ITEM_ID);
		private RouteSegmentCreate insertedRoute;
		private RouteMatchRequestLog insertedLog;

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
		public Optional<ItineraryDayReadModel> updateDay(
			com.soomgil.itinerary.application.port.ItineraryDayUpdate update
		) {
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
		public Optional<com.soomgil.itinerary.application.port.ItineraryItemReadModel> findItem(UUID tripId, UUID itemId) {
			return Optional.empty();
		}

		@Override
		public Optional<com.soomgil.itinerary.application.port.ItineraryItemReadModel> updateItem(
			com.soomgil.itinerary.application.port.ItineraryItemUpdate update
		) {
			return Optional.empty();
		}

		@Override
		public void insertMapDrawing(MapDrawingCreate drawing) {
		}

		@Override
		public void insertRouteSegment(RouteSegmentCreate route) {
			this.insertedRoute = route;
		}

		@Override
		public java.util.Optional<com.soomgil.itinerary.application.port.RouteSegmentUpdateResult> updateRouteSegment(
			com.soomgil.itinerary.application.port.RouteSegmentUpdate update
		) {
			return java.util.Optional.empty();
		}
		@Override
		public Long insertRouteMatchRequest(RouteMatchRequestLog request) {
			this.insertedLog = request;
			return 11L;
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
			return true;
		}

		@Override
		public long countDays(UUID tripId) {
			return 1;
		}

		@Override
		public boolean existsItem(UUID tripId, UUID itemId) {
			return itemIds.contains(itemId);
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
			return itemIds.size();
		}

		@Override
		public void updateDayOrder(ItineraryDayOrderUpdate update) {
		}

		@Override
		public void updateItemOrder(ItineraryItemOrderUpdate update) {
		}
	}

	private static class CapturingEventRepository implements CollaborationCommandEventRepository {

		@Override
		public void save(CollaborationCommandEvent event) {
		}
	}
}
