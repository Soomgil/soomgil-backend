package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryDayCommand;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryDayUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.application.port.MapDrawingCreate;
import com.soomgil.itinerary.application.port.MapDrawingUpdate;
import com.soomgil.itinerary.application.port.MapDrawingUpdateResult;
import com.soomgil.itinerary.application.port.RouteMatchRequestLog;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateItineraryDayHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final UpdateItineraryDayHandler handler = new UpdateItineraryDayHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new CreateItineraryDayHandlerTest.StubTripQueryRepository()),
		() -> Instant.parse("2026-06-17T00:00:00Z")
	);

	@Test
	void updatesDayAndRecordsEvent() {
		ItineraryMutationResult result = handler.handle(new UpdateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			2,
			LocalDate.parse("2026-07-02"),
			"  둘째 날  ",
			5
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.day().id()).isEqualTo(DAY_ID);
		assertThat(result.day().dayNumber()).isEqualTo(2);
		assertThat(result.day().title()).isEqualTo("둘째 날");
		assertThat(repository.lastUpdate.sortOrder()).isEqualTo(5);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("UPDATE_ITINERARY_DAY");
	}

	@Test
	void rejectsUnscheduledDate() {
		repository.currentDay = new ItineraryDayReadModel(
			DAY_ID,
			TRIP_ID,
			ItineraryDayGroupType.UNSCHEDULED,
			null,
			null,
			"일차 미정",
			0
		);

		assertThatThrownBy(() -> handler.handle(new UpdateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			null,
			LocalDate.parse("2026-07-02"),
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION)
		);
	}

	@Test
	void rejectsMissingFields() {
		assertThatThrownBy(() -> handler.handle(new UpdateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			DAY_ID,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	private static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private ItineraryDayReadModel currentDay = new ItineraryDayReadModel(
			DAY_ID,
			TRIP_ID,
			ItineraryDayGroupType.DAY,
			1,
			LocalDate.parse("2026-07-01"),
			"첫째 날",
			0
		);
		private ItineraryDayUpdate lastUpdate;

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
			return Optional.ofNullable(currentDay);
		}

		@Override
		public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public Optional<ItineraryDayReadModel> updateDay(ItineraryDayUpdate update) {
			this.lastUpdate = update;
			currentDay = new ItineraryDayReadModel(
				update.dayId(),
				update.tripId(),
				currentDay.groupType(),
				update.dayNumber(),
				update.date(),
				update.title(),
				update.sortOrder()
			);
			return Optional.of(currentDay);
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
