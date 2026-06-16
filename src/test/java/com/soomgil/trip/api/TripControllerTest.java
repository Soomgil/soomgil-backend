package com.soomgil.trip.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.api.dto.CreateTripRequest;
import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripDetail;
import com.soomgil.trip.api.dto.TripStatus;
import com.soomgil.trip.application.command.handler.CreateTripHandler;
import com.soomgil.trip.application.command.handler.CreateTripInviteHandler;
import com.soomgil.trip.application.command.handler.RevokeTripInviteHandler;
import com.soomgil.trip.application.port.TripInviteCodeGenerator;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.application.query.handler.ListTripInvitesHandler;
import com.soomgil.trip.application.query.handler.ListMyTripsHandler;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripInvite;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripControllerTest {

	@Test
	void createTripReturnsOwnerTripDetail() {
		UUID currentUserId = UUID.randomUUID();
		TripController controller = controller();
		CreateTripRequest request = new CreateTripRequest("서울 여행", "서울", List.of(), null, null);

		TripDetail detail = controller.createTrip(request, principal(currentUserId));

		assertThat(detail.id()).isNotNull();
		assertThat(detail.ownerUserId()).isEqualTo(currentUserId);
		assertThat(detail.title()).isEqualTo("서울 여행");
		assertThat(detail.displayDestination()).isEqualTo("서울");
		assertThat(detail.status()).isEqualTo(TripStatus.ACTIVE);
		assertThat(detail.myRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(detail.itineraryVersion()).isZero();
		assertThat(detail.members()).hasSize(1);
		assertThat(detail.members().get(0).user().id()).isEqualTo(currentUserId);
		assertThat(detail.members().get(0).accessRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(detail.regions()).isEmpty();
	}

	private static TripController controller() {
		TripQueryRepository queryRepository = new EmptyTripQueryRepository();
		TripAccessGuard accessGuard = new TripAccessGuard(queryRepository);
		return new TripController(
			new CreateTripHandler(new NoopTripCommandRepository(), fixedTime()),
			new CreateTripInviteHandler(new NoopTripCommandRepository(), queryRepository, fixedTime(), fixedCodeGenerator()),
			new RevokeTripInviteHandler(new NoopTripCommandRepository(), queryRepository, fixedTime()),
			new ListMyTripsHandler(queryRepository),
			new FindTripDetailHandler(accessGuard, queryRepository),
			new ListTripMembersHandler(accessGuard, queryRepository),
			new ListTripInvitesHandler(accessGuard, queryRepository)
		);
	}

	private static Principal principal(UUID userId) {
		return userId::toString;
	}

	private static TimeProvider fixedTime() {
		return () -> Instant.parse("2026-06-16T00:00:00Z");
	}

	private static TripInviteCodeGenerator fixedCodeGenerator() {
		return () -> "ABCD1234";
	}

	private static class NoopTripCommandRepository implements TripCommandRepository {

		@Override
		public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
		}

		@Override
		public void saveTripInvite(TripInvite invite) {
		}

		@Override
		public void revokeTripInvite(UUID inviteId, UUID revokedByUserId, Instant revokedAt) {
		}
	}

	private static class EmptyTripQueryRepository implements TripQueryRepository {

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
			com.soomgil.trip.domain.model.TripStatus status,
			com.soomgil.trip.domain.model.TripAccessRole role,
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
