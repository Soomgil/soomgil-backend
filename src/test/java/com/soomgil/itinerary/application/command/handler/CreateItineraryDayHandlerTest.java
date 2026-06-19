package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
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
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripInviteAcceptReadModel;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateItineraryDayHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID UNSCHEDULED_DAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000099");
	private static final Instant NOW = Instant.parse("2026-06-17T00:00:00Z");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CapturingEventRepository eventRepository = new CapturingEventRepository();
	private final CreateItineraryDayHandler handler = new CreateItineraryDayHandler(
		repository,
		eventRepository,
		new TripAccessGuard(new StubTripQueryRepository()),
		() -> NOW
	);

	@Test
	void createsDayAndIncrementsVersion() {
		ItineraryMutationResult result = handler.handle(new CreateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			ItineraryDayGroupType.DAY,
			1,
			LocalDate.parse("2026-07-01"),
			"  첫째 날  ",
			3
		));

		assertThat(result.itineraryVersion()).isEqualTo(1);
		assertThat(result.day().tripId()).isEqualTo(TRIP_ID);
		assertThat(result.day().dayNumber()).isEqualTo(1);
		assertThat(result.day().title()).isEqualTo("첫째 날");
		assertThat(repository.insertedDay.sortOrder()).isEqualTo(3);
		assertThat(eventRepository.lastEvent.commandType()).isEqualTo("CREATE_ITINERARY_DAY");
		assertThat(eventRepository.lastEvent.redoPayload()).contains(
			"RESTORE_ITINERARY_DAY",
			result.day().id().toString(),
			"DAY"
		);
		assertThat(eventRepository.lastEvent.versionBefore()).isEqualTo(0);
		assertThat(eventRepository.lastEvent.versionAfter()).isEqualTo(1);
	}

	@Test
	void rejectsVersionConflict() {
		repository.currentVersion = 2;

		assertThatThrownBy(() -> handler.handle(new CreateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			ItineraryDayGroupType.DAY,
			1,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
		);
	}

	@Test
	void rejectsDayWithoutDayNumber() {
		assertThatThrownBy(() -> handler.handle(new CreateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			ItineraryDayGroupType.DAY,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void rejectsMissingGroupType() {
		assertThatThrownBy(() -> handler.handle(new CreateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			0,
			null,
			1,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
		);
	}

	@Test
	void reusesExistingUnscheduledDayWithoutIncrementingVersion() {
		repository.currentVersion = 5;
		repository.unscheduledDay = new ItineraryDayReadModel(
			UNSCHEDULED_DAY_ID,
			TRIP_ID,
			ItineraryDayGroupType.UNSCHEDULED,
			null,
			null,
			"일차 미정",
			99
		);

		ItineraryMutationResult result = handler.handle(new CreateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			5,
			ItineraryDayGroupType.UNSCHEDULED,
			null,
			null,
			null,
			null
		));

		assertThat(result.itineraryVersion()).isEqualTo(5);
		assertThat(result.day().id()).isEqualTo(UNSCHEDULED_DAY_ID);
		assertThat(repository.insertedDay).isNull();
		assertThat(eventRepository.lastEvent).isNull();
	}

	@Test
	void rejectsStaleVersionWhenReusingUnscheduledDay() {
		repository.currentVersion = 5;
		repository.unscheduledDay = new ItineraryDayReadModel(
			UNSCHEDULED_DAY_ID,
			TRIP_ID,
			ItineraryDayGroupType.UNSCHEDULED,
			null,
			null,
			null,
			0
		);

		assertThatThrownBy(() -> handler.handle(new CreateItineraryDayCommand(
			TRIP_ID,
			USER_ID,
			4,
			ItineraryDayGroupType.UNSCHEDULED,
			null,
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
		);
	}

	private static class CapturingEventRepository implements CollaborationCommandEventRepository {

		private CollaborationCommandEvent lastEvent;

		@Override
		public void save(CollaborationCommandEvent event) {
			this.lastEvent = event;
		}
	}

	static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private ItineraryDayCreate insertedDay;
		private ItineraryDayReadModel unscheduledDay;

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
			this.insertedDay = day;
		}

		@Override
		public Optional<ItineraryDayReadModel> findDay(UUID tripId, UUID dayId) {
			return Optional.empty();
		}

		@Override
		public Optional<ItineraryDayReadModel> findUnscheduledDay(UUID tripId) {
			return Optional.ofNullable(unscheduledDay);
		}

		@Override
		public Optional<ItineraryDayReadModel> updateDay(com.soomgil.itinerary.application.port.ItineraryDayUpdate update) {
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

	static class StubTripQueryRepository implements TripQueryRepository {

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return Optional.of(new TripAccessSnapshot(
				tripId,
				userId,
				TripStatus.ACTIVE,
				TripMemberStatus.ACTIVE,
				userId
			));
		}

		@Override
		public Optional<TripReadModel> findTrip(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public List<TripMemberReadModel> findTripMembers(UUID tripId, TripMemberStatus status) {
			return List.of();
		}

		@Override
		public TripSummaryPage findMyTrips(
			UUID userId,
			TripStatus status,
			TripAccessRole role,
			int page,
			int size,
			List<String> sort
		) {
			return new TripSummaryPage(List.of(), 0);
		}

		@Override
		public List<TripInviteReadModel> findTripInvites(UUID tripId, InviteStatus status) {
			return List.of();
		}

		@Override
		public Optional<TripInviteAcceptReadModel> findTripInviteForAccept(String inviteCode) {
			return Optional.empty();
		}
	}
}
