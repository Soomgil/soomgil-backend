package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
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
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateItineraryItemHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID ITEM_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final UUID ROUTE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final UpdateItineraryItemHandler handler = new UpdateItineraryItemHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z")
	);

	@Test
	void updatesItemAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new UpdateItineraryItemCommand(
			TRIP_ID,
			USER_ID,
			0,
			ITEM_ID,
			DAY_ID,
			2,
			"  수정 장소  ",
			"  새 주소  ",
			36.1,
			127.1,
			URI.create("https://example.com/thumb.jpg")
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.item().id()).isEqualTo(ITEM_ID);
		assertThat(result.item().placeName()).isEqualTo("수정 장소");
		assertThat(repository.lastUpdate.sortOrder()).isEqualTo(2);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("UPDATE_ITINERARY_ITEM");
	}

	@Test
	void rejectsMovingRouteConnectedItem() {
		repository.connectedRouteIds = List.of(ROUTE_ID);

		assertThatThrownBy(() -> handler.handle(new UpdateItineraryItemCommand(
			TRIP_ID,
			USER_ID,
			0,
			ITEM_ID,
			UUID.fromString("30000000-0000-0000-0000-000000000002"),
			null,
			null,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION)
		);
	}

	@Test
	void allowsDisplayUpdateForRouteConnectedItem() {
		repository.connectedRouteIds = List.of(ROUTE_ID);

		ItineraryMutationResult result = handler.handle(new UpdateItineraryItemCommand(
			TRIP_ID,
			USER_ID,
			0,
			ITEM_ID,
			null,
			null,
			"표시명",
			null,
			null,
			null,
			null
		));

		assertThat(result.item().placeName()).isEqualTo("표시명");
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private List<UUID> connectedRouteIds = List.of();
		private ItineraryItemUpdate lastUpdate;
		private ItineraryItemReadModel currentItem = new ItineraryItemReadModel(
			ITEM_ID,
			DAY_ID,
			0,
			ItineraryItemType.PLACE,
			"KTO",
			"126508",
			"성심당",
			"대전",
			36.0,
			127.0,
			null,
			"AVAILABLE"
		);

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
		public void insertItem(ItineraryItemCreate item) {
		}

		@Override
		public Optional<ItineraryItemReadModel> findItem(UUID tripId, UUID itemId) {
			return Optional.ofNullable(currentItem);
		}

		@Override
		public Optional<ItineraryItemReadModel> updateItem(ItineraryItemUpdate update) {
			lastUpdate = update;
			currentItem = new ItineraryItemReadModel(
				update.itemId(),
				update.itineraryDayId(),
				update.sortOrder(),
				currentItem.itemType(),
				currentItem.placeProvider(),
				currentItem.externalPlaceId(),
				update.placeName(),
				update.address(),
				update.lat(),
				update.lng(),
				update.thumbnailUrl() == null ? null : URI.create(update.thumbnailUrl()),
				currentItem.sourceStatus()
			);
			return Optional.of(currentItem);
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
			return currentItem != null;
		}

		@Override
		public List<UUID> findActiveRouteIdsByItem(UUID tripId, UUID itemId) {
			return connectedRouteIds;
		}

		@Override
		public boolean softDeleteItem(UUID tripId, UUID itemId, UUID deletedByUserId, Instant deletedAt) {
			return false;
		}

		@Override
		public long countActiveItems(UUID tripId) {
			return currentItem == null ? 0 : 1;
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
			lastEvent = event;
		}
	}
}
