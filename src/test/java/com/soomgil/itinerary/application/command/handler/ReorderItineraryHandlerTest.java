package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryDayOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.ReorderItineraryCommand;
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
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReorderItineraryHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
	private static final UUID ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final ReorderItineraryHandler handler = new ReorderItineraryHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z")
	);

	@Test
	void reordersDaysAndMovesItems() {
		ItineraryMutationResult result = handler.handle(new ReorderItineraryCommand(
			TRIP_ID,
			USER_ID,
			0,
			List.of(
				new ItineraryDayOrderCommand(DAY_ID, 1, List.of()),
				new ItineraryDayOrderCommand(OTHER_DAY_ID, 0, List.of(new ItineraryItemOrderCommand(ITEM_ID, 2)))
			)
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(repository.dayUpdates).extracting(ItineraryDayOrderUpdate::dayId)
			.containsExactly(DAY_ID, OTHER_DAY_ID);
		assertThat(repository.itemUpdates).hasSize(1);
		assertThat(repository.itemUpdates.get(0).dayId()).isEqualTo(OTHER_DAY_ID);
		assertThat(repository.itemUpdates.get(0).sortOrder()).isEqualTo(2);
		assertThat(repository.itemUpdates.get(0).updatedByUserId()).isEqualTo(USER_ID);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("REORDER_ITINERARY");
		assertThat(eventRepository.lastEvent.payload()).contains(ITEM_ID.toString());
	}

	@Test
	void rejectsDuplicateItems() {
		assertThatThrownBy(() -> handler.handle(new ReorderItineraryCommand(
			TRIP_ID,
			USER_ID,
			0,
			List.of(
				new ItineraryDayOrderCommand(DAY_ID, 0, List.of(new ItineraryItemOrderCommand(ITEM_ID, 0))),
				new ItineraryDayOrderCommand(OTHER_DAY_ID, 1, List.of(new ItineraryItemOrderCommand(ITEM_ID, 0)))
			)
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	private static class CapturingEventRepository implements CollaborationCommandEventRepository {

		private CollaborationCommandEvent lastEvent;

		@Override
		public void save(CollaborationCommandEvent event) {
			this.lastEvent = event;
		}
	}

	@Test
	void rejectsMissingItem() {
		repository.itemIds = Set.of();

		assertThatThrownBy(() -> handler.handle(new ReorderItineraryCommand(
			TRIP_ID,
			USER_ID,
			0,
			List.of(new ItineraryDayOrderCommand(DAY_ID, 0, List.of(new ItineraryItemOrderCommand(ITEM_ID, 0))))
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
		);
	}

	@Test
	void rejectsPartialDaySnapshot() {
		assertThatThrownBy(() -> handler.handle(new ReorderItineraryCommand(
			TRIP_ID,
			USER_ID,
			0,
			List.of(new ItineraryDayOrderCommand(DAY_ID, 0, List.of(new ItineraryItemOrderCommand(ITEM_ID, 0))))
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsPartialItemSnapshot() {
		repository.itemIds = Set.of(ITEM_ID, UUID.fromString("40000000-0000-0000-0000-000000000002"));

		assertThatThrownBy(() -> handler.handle(new ReorderItineraryCommand(
			TRIP_ID,
			USER_ID,
			0,
			List.of(
				new ItineraryDayOrderCommand(DAY_ID, 0, List.of(new ItineraryItemOrderCommand(ITEM_ID, 0))),
				new ItineraryDayOrderCommand(OTHER_DAY_ID, 1, List.of())
			)
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsVersionConflict() {
		repository.currentVersion = 2;

		assertThatThrownBy(() -> handler.handle(new ReorderItineraryCommand(
			TRIP_ID,
			USER_ID,
			0,
			List.of(
				new ItineraryDayOrderCommand(DAY_ID, 0, List.of(new ItineraryItemOrderCommand(ITEM_ID, 0))),
				new ItineraryDayOrderCommand(OTHER_DAY_ID, 1, List.of())
			)
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private Set<UUID> dayIds = Set.of(DAY_ID, OTHER_DAY_ID);
		private Set<UUID> itemIds = Set.of(ITEM_ID);
		private final List<ItineraryDayOrderUpdate> dayUpdates = new ArrayList<>();
		private final List<ItineraryItemOrderUpdate> itemUpdates = new ArrayList<>();

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
		public java.util.Optional<ItineraryDayReadModel> findDay(UUID tripId, UUID dayId) {
			return java.util.Optional.empty();
		}

		@Override
		public java.util.Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
			return java.util.Optional.empty();
		}

		@Override
		public java.util.Optional<ItineraryDayReadModel> updateDay(
			com.soomgil.itinerary.application.port.ItineraryDayUpdate update
		) {
			return java.util.Optional.empty();
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
		public java.util.Optional<com.soomgil.itinerary.application.port.ItineraryItemReadModel> findItem(UUID tripId, UUID itemId) {
			return java.util.Optional.empty();
		}

		@Override
		public java.util.Optional<com.soomgil.itinerary.application.port.ItineraryItemReadModel> updateItem(
			com.soomgil.itinerary.application.port.ItineraryItemUpdate update
		) {
			return java.util.Optional.empty();
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
		public java.util.Optional<com.soomgil.itinerary.application.port.MapDrawingUpdateResult> updateMapDrawing(
			com.soomgil.itinerary.application.port.MapDrawingUpdate update
		) {
			return java.util.Optional.empty();
		}

		@Override
		public boolean existsDay(UUID tripId, UUID dayId) {
			return dayIds.contains(dayId);
		}

		@Override
		public long countDays(UUID tripId) {
			return dayIds.size();
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
			dayUpdates.add(update);
		}

		@Override
		public void updateItemOrder(ItineraryItemOrderUpdate update) {
			itemUpdates.add(update);
		}
	}
}
