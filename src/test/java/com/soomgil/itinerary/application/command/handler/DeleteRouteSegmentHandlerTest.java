package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.DeleteRouteSegmentCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeleteRouteSegmentHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ROUTE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final DeleteRouteSegmentHandler handler = new DeleteRouteSegmentHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z")
	);

	@Test
	void softDeletesRouteSegmentAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new DeleteRouteSegmentCommand(TRIP_ID, USER_ID, 0, ROUTE_ID));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.affectedRouteIds()).containsExactly(ROUTE_ID);
		assertThat(repository.deletedRouteId).isEqualTo(ROUTE_ID);
		assertThat(repository.deletedByUserId).isEqualTo(USER_ID);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("DELETE_ROUTE_SEGMENT");
		assertThat(eventRepository.lastEvent.aggregateId()).isEqualTo(ROUTE_ID);
		assertThat(eventRepository.lastEvent.inversePayload()).contains("RESTORE_ROUTE_SEGMENT");
		assertThat(eventRepository.lastEvent.redoPayload()).contains("DELETE_ROUTE_SEGMENT");
	}

	@Test
	void rejectsMissingRoute() {
		repository.routeExists = false;

		assertThatThrownBy(() -> handler.handle(new DeleteRouteSegmentCommand(TRIP_ID, USER_ID, 0, ROUTE_ID)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
			);
		assertThat(repository.currentVersion).isZero();
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private boolean routeExists = true;
		private UUID deletedRouteId;
		private UUID deletedByUserId;

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
		}

		@Override
		public java.util.Optional<com.soomgil.itinerary.application.port.RouteSegmentUpdateResult> updateRouteSegment(
			com.soomgil.itinerary.application.port.RouteSegmentUpdate update
		) {
			return java.util.Optional.empty();
		}
		@Override
		public Long insertRouteMatchRequest(RouteMatchRequestLog request) {
			return 1L;
		}

		@Override
		public boolean existsActiveRouteSegment(UUID tripId, UUID routeId) {
			return routeExists;
		}

		@Override
		public boolean softDeleteRouteSegment(UUID tripId, UUID routeId, UUID deletedByUserId, Instant deletedAt) {
			if (!routeExists) {
				return false;
			}
			this.deletedRouteId = routeId;
			this.deletedByUserId = deletedByUserId;
			routeExists = false;
			return true;
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
			return false;
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
