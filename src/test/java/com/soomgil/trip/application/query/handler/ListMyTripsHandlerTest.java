package com.soomgil.trip.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.dto.ListMyTripsQuery;
import com.soomgil.trip.application.query.dto.PagedTripSummaryView;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListMyTripsHandlerTest {

	private final UUID userId = UUID.randomUUID();
	private final StubTripQueryRepository repository = new StubTripQueryRepository();
	private final ListMyTripsHandler handler = new ListMyTripsHandler(repository);

	@Test
	void listsTripsWhereCurrentUserIsActiveMember() {
		UUID ownedTripId = UUID.randomUUID();
		UUID memberTripId = UUID.randomUUID();
		repository.page = new TripSummaryPage(List.of(
			trip(ownedTripId, userId),
			trip(memberTripId, UUID.randomUUID())
		), 2);

		PagedTripSummaryView page = handler.handle(new ListMyTripsQuery(
			userId,
			TripStatus.ACTIVE,
			null,
			0,
			20,
			List.of("createdAt,desc")
		));

		assertThat(page.items()).hasSize(2);
		assertThat(page.items().get(0).myRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(page.items().get(1).myRole()).isEqualTo(TripAccessRole.MEMBER);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(repository.status).isEqualTo(TripStatus.ACTIVE);
		assertThat(repository.pageNumber).isZero();
		assertThat(repository.pageSize).isEqualTo(20);
	}

	private TripReadModel trip(UUID tripId, UUID ownerUserId) {
		return new TripReadModel(
			tripId,
			ownerUserId,
			"여행",
			null,
			TripStatus.ACTIVE,
			0,
			Instant.parse("2026-06-16T00:00:00Z"),
			null
		);
	}

	private static class StubTripQueryRepository implements TripQueryRepository {

		private TripSummaryPage page = new TripSummaryPage(List.of(), 0);
		private TripStatus status;
		private int pageNumber;
		private int pageSize;

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return Optional.empty();
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
			this.status = status;
			this.pageNumber = page;
			this.pageSize = size;
			return this.page;
		}

		@Override
		public List<TripInviteReadModel> findTripInvites(UUID tripId, InviteStatus status) {
			return List.of();
		}
	}
}
