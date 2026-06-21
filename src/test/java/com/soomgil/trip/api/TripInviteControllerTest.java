package com.soomgil.trip.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripDetail;
import com.soomgil.trip.application.command.handler.AcceptTripInviteHandler;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripInviteAcceptReadModel;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSettingsUpdate;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripInvite;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripInviteControllerTest {

	@Test
	void acceptTripInviteReturnsJoinedTripDetail() {
		AcceptState state = new AcceptState();
		TripQueryRepository queryRepository = new AcceptQueryRepository(state);
		TripCommandRepository commandRepository = new AcceptCommandRepository(state);
		TripInviteController controller = new TripInviteController(
			new AcceptTripInviteHandler(commandRepository, queryRepository, fixedTime()),
			new FindTripDetailHandler(new TripAccessGuard(queryRepository), queryRepository)
		);

		TripDetail detail = controller.acceptTripInvite("ABCD1234", null, principal(state.actorUserId));

		assertThat(detail.id()).isEqualTo(state.tripId);
		assertThat(detail.myRole()).isEqualTo(TripAccessRole.MEMBER);
		assertThat(detail.members()).extracting(member -> member.user().id()).contains(state.actorUserId);
	}

	private static Principal principal(UUID userId) {
		return userId::toString;
	}

	private static TimeProvider fixedTime() {
		return () -> Instant.parse("2026-06-16T00:00:00Z");
	}

	private static class AcceptState {

		private final UUID tripId = UUID.randomUUID();
		private final UUID inviteId = UUID.randomUUID();
		private final UUID ownerUserId = UUID.randomUUID();
		private final UUID actorUserId = UUID.randomUUID();
		private boolean accepted;
		private TripMember member;
	}

	private static class AcceptCommandRepository implements TripCommandRepository {

		private final AcceptState state;

		private AcceptCommandRepository(AcceptState state) {
			this.state = state;
		}

		@Override
		public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
		}

		@Override
		public void saveCreatedRetrip(Trip trip, TripMember initialMember, UUID sourcePostId, int snapshotVersion) {
		}

		@Override
		public void saveTripInvite(TripInvite invite) {
		}

		@Override
		public boolean revokeTripInvite(UUID inviteId, UUID revokedByUserId, Instant revokedAt) {
			return true;
		}

		@Override
		public void addTripMember(TripMember member) {
			state.member = member;
		}

		@Override
		public boolean acceptTripInvite(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt) {
			state.accepted = true;
			return true;
		}

		@Override
		public void updateTrip(TripSettingsUpdate update) {
		}

		@Override
		public void replaceTripRegions(UUID tripId, List<String> legalRegionCodes, Instant createdAt) {
		}

		@Override
		public void softDeleteTrip(UUID tripId, Instant deletedAt) {
		}

		@Override
		public void removeTripMember(UUID tripId, UUID userId, UUID removedByUserId, Instant removedAt) {
		}
	}

	private static class AcceptQueryRepository implements TripQueryRepository {

		private final AcceptState state;

		private AcceptQueryRepository(AcceptState state) {
			this.state = state;
		}

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			TripMemberStatus memberStatus = state.member == null ? null : TripMemberStatus.ACTIVE;
			return Optional.of(new TripAccessSnapshot(
				state.tripId,
				userId,
				TripStatus.ACTIVE,
				memberStatus,
				state.ownerUserId
			));
		}

		@Override
		public Optional<TripReadModel> findTrip(UUID tripId) {
			return Optional.of(new TripReadModel(
				state.tripId,
				state.ownerUserId,
				"서울 여행",
				"서울",
				TripStatus.ACTIVE,
				0,
				Instant.parse("2026-06-16T00:00:00Z"),
				null
			));
		}

		@Override
		public List<TripMemberReadModel> findTripMembers(UUID tripId, TripMemberStatus status) {
			if (state.member == null) {
				return List.of();
			}
			return List.of(new TripMemberReadModel(
				state.member.id(),
				state.tripId,
				state.actorUserId,
				TripMemberRole.MEMBER,
				TripMemberStatus.ACTIVE,
				state.member.joinedAt(),
				state.ownerUserId
			));
		}

		@Override
		public TripSummaryPage findMyTrips(
			UUID userId,
			TripStatus status,
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

		@Override
		public Optional<TripInviteAcceptReadModel> findTripInviteForAccept(String inviteCode) {
			return Optional.of(new TripInviteAcceptReadModel(
				state.inviteId,
				state.tripId,
				inviteCode,
				null,
				state.accepted ? InviteStatus.ACCEPTED : InviteStatus.PENDING,
				null,
				state.ownerUserId,
				TripStatus.ACTIVE
			));
		}
	}
}
