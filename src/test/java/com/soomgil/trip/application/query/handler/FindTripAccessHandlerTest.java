package com.soomgil.trip.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.dto.FindTripAccessQuery;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindTripAccessHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();
	private final StubTripQueryRepository repository = new StubTripQueryRepository();
	private final FindTripAccessHandler handler = new FindTripAccessHandler(repository);

	@Test
	void ownerActiveMemberCanAccessTripAsOwner() {
		repository.snapshot = Optional.of(new TripAccessSnapshot(
			tripId,
			userId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			userId
		));

		TripAccessView view = handler.handle(new FindTripAccessQuery(tripId, userId));

		assertThat(view.tripId()).isEqualTo(tripId);
		assertThat(view.userId()).isEqualTo(userId);
		assertThat(view.canAccess()).isTrue();
		assertThat(view.owner()).isTrue();
		assertThat(view.accessRole()).isEqualTo(TripAccessRole.OWNER);
	}

	@Test
	void activeNonOwnerMemberCanAccessTripAsMember() {
		repository.snapshot = Optional.of(new TripAccessSnapshot(
			tripId,
			userId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			UUID.randomUUID()
		));

		TripAccessView view = handler.handle(new FindTripAccessQuery(tripId, userId));

		assertThat(view.canAccess()).isTrue();
		assertThat(view.owner()).isFalse();
		assertThat(view.accessRole()).isEqualTo(TripAccessRole.MEMBER);
	}

	@Test
	void nonMemberCannotAccessTrip() {
		repository.snapshot = Optional.of(new TripAccessSnapshot(
			tripId,
			userId,
			TripStatus.ACTIVE,
			null,
			UUID.randomUUID()
		));

		TripAccessView view = handler.handle(new FindTripAccessQuery(tripId, userId));

		assertThat(view.canAccess()).isFalse();
		assertThat(view.owner()).isFalse();
		assertThat(view.accessRole()).isNull();
	}

	@Test
	void deletedTripCannotBeAccessedEvenByActiveMember() {
		repository.snapshot = Optional.of(new TripAccessSnapshot(
			tripId,
			userId,
			TripStatus.DELETED,
			TripMemberStatus.ACTIVE,
			userId
		));

		TripAccessView view = handler.handle(new FindTripAccessQuery(tripId, userId));

		assertThat(view.canAccess()).isFalse();
		assertThat(view.owner()).isFalse();
		assertThat(view.accessRole()).isNull();
	}

	@Test
	void missingTripCannotBeAccessed() {
		repository.snapshot = Optional.empty();

		TripAccessView view = handler.handle(new FindTripAccessQuery(tripId, userId));

		assertThat(view.canAccess()).isFalse();
		assertThat(view.owner()).isFalse();
		assertThat(view.accessRole()).isNull();
	}

	private static class StubTripQueryRepository implements TripQueryRepository {

		private Optional<TripAccessSnapshot> snapshot = Optional.empty();

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return snapshot;
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
	}
}
