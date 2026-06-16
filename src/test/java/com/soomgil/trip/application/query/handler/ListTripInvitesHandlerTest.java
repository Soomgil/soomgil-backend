package com.soomgil.trip.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.dto.ListTripInvitesQuery;
import com.soomgil.trip.application.query.dto.TripInviteView;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListTripInvitesHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID memberUserId = UUID.randomUUID();
	private final StubTripQueryRepository repository = new StubTripQueryRepository();
	private final ListTripInvitesHandler handler = new ListTripInvitesHandler(new TripAccessGuard(repository), repository);

	@Test
	void ownerCanListTripInvites() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			ownerUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));
		repository.invites = List.of(new TripInviteReadModel(
			UUID.randomUUID(),
			tripId,
			"ABCD1234",
			null,
			InviteStatus.PENDING,
			null,
			Instant.parse("2026-06-16T00:00:00Z")
		));

		List<TripInviteView> invites = handler.handle(new ListTripInvitesQuery(
			tripId,
			ownerUserId,
			InviteStatus.PENDING
		));

		assertThat(invites).hasSize(1);
		assertThat(invites.get(0).inviteCode()).isEqualTo("ABCD1234");
		assertThat(invites.get(0).status()).isEqualTo(InviteStatus.PENDING);
	}

	@Test
	void nonOwnerCannotListTripInvites() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new ListTripInvitesQuery(tripId, memberUserId, null)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	private static class StubTripQueryRepository implements TripQueryRepository {

		private Optional<TripAccessSnapshot> access = Optional.empty();
		private List<TripInviteReadModel> invites = List.of();

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return access;
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
			return invites;
		}
	}
}
