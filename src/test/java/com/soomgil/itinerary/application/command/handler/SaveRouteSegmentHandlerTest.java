package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.SaveRouteSegmentCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SaveRouteSegmentHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ORIGIN_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID DESTINATION_ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final SaveRouteSegmentHandler handler = new SaveRouteSegmentHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z"),
		new ObjectMapper()
	);

	@Test
	void savesRouteSegmentAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new SaveRouteSegmentCommand(
			TRIP_ID,
			USER_ID,
			0,
			ORIGIN_ITEM_ID,
			DESTINATION_ITEM_ID,
			RouteMode.WALKING,
			null,
			null,
			Map.of("type", "LineString", "coordinates", java.util.List.of()),
			123.4,
			56.7,
			0.91
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.route().id()).isEqualTo(repository.insertedRoute.id());
		assertThat(result.route().providerProfile()).isEqualTo("mapbox/walking");
		assertThat(repository.insertedRoute.geometry()).contains("LineString");
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("CREATE_ROUTE_SEGMENT");
		assertThat(eventRepository.lastEvent.aggregateId()).isEqualTo(repository.insertedRoute.id());
	}

	@Test
	void rejectsSameOriginAndDestination() {
		assertThatThrownBy(() -> handler.handle(new SaveRouteSegmentCommand(
			TRIP_ID,
			USER_ID,
			0,
			ORIGIN_ITEM_ID,
			ORIGIN_ITEM_ID,
			RouteMode.DRIVING,
			null,
			null,
			Map.of("type", "LineString"),
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsMissingItineraryItem() {
		repository.itemIds = Set.of(ORIGIN_ITEM_ID);

		assertThatThrownBy(() -> handler.handle(new SaveRouteSegmentCommand(
			TRIP_ID,
			USER_ID,
			0,
			ORIGIN_ITEM_ID,
			DESTINATION_ITEM_ID,
			RouteMode.DRIVING,
			null,
			null,
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
		private Set<UUID> itemIds = Set.of(ORIGIN_ITEM_ID, DESTINATION_ITEM_ID);
		private RouteSegmentCreate insertedRoute;

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
			return false;
		}

		@Override
		public boolean softDeleteMapDrawing(UUID tripId, UUID drawingId, UUID deletedByUserId, Instant deletedAt) {
			return false;
		}

		@Override
		public Optional<com.soomgil.itinerary.application.port.MapDrawingUpdateResult> updateMapDrawing(
			com.soomgil.itinerary.application.port.MapDrawingUpdate update
		) {
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
			return itemIds.contains(itemId);
		}

		@Override
		public java.util.List<UUID> findActiveRouteIdsByItem(UUID tripId, UUID itemId) {
			return java.util.List.of();
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

		private CollaborationCommandEvent lastEvent;

		@Override
		public void save(CollaborationCommandEvent event) {
			this.lastEvent = event;
		}
	}
}
