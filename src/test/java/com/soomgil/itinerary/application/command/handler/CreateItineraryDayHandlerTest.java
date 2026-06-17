package com.soomgil.itinerary.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
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
	private static final Instant NOW = Instant.parse("2026-06-17T00:00:00Z");

	private final CapturingItineraryCommandRepository repository = new CapturingItineraryCommandRepository();
	private final CreateItineraryDayHandler handler = new CreateItineraryDayHandler(
		repository,
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

	static class CapturingItineraryCommandRepository implements ItineraryCommandRepository {

		private long currentVersion;
		private ItineraryDayCreate insertedDay;

		@Override
		public OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt) {
			if (currentVersion != baseVersion) {
				return OptionalLong.empty();
			}
			currentVersion++;
			return OptionalLong.of(currentVersion);
		}

		@Override
		public void insertDay(ItineraryDayCreate day) {
			this.insertedDay = day;
		}

		@Override
		public void insertItem(ItineraryItemCreate item) {
		}

		@Override
		public boolean existsDay(UUID tripId, UUID dayId) {
			return false;
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
