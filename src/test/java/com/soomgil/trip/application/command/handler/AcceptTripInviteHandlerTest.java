package com.soomgil.trip.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.AcceptTripInviteCommand;
import com.soomgil.trip.application.command.dto.AcceptTripInviteResult;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripInviteAcceptReadModel;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripSettingsUpdate;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripInvite;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcceptTripInviteHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID inviteId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID actorUserId = UUID.randomUUID();
	private final CapturingTripCommandRepository commandRepository = new CapturingTripCommandRepository();
	private final StubTripQueryRepository queryRepository = new StubTripQueryRepository();
	private final AcceptTripInviteHandler handler = new AcceptTripInviteHandler(
		commandRepository,
		queryRepository,
		fixedTime()
	);

	@Test
	void acceptsPendingInviteAndAddsActiveMember() {
		queryRepository.invite = Optional.of(invite(actorUserId, InviteStatus.PENDING, null));
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			actorUserId,
			TripStatus.ACTIVE,
			null,
			ownerUserId
		));

		AcceptTripInviteResult result = handler.handle(new AcceptTripInviteCommand("ABCD1234", actorUserId));

		assertThat(result.tripId()).isEqualTo(tripId);
		assertThat(commandRepository.acceptedInviteId).isEqualTo(inviteId);
		assertThat(commandRepository.acceptedByUserId).isEqualTo(actorUserId);
		assertThat(commandRepository.addedMember.tripId()).isEqualTo(tripId);
		assertThat(commandRepository.addedMember.userId()).isEqualTo(actorUserId);
		assertThat(commandRepository.addedMember.status()).isEqualTo(TripMemberStatus.ACTIVE);
	}

	@Test
	void rejectsInviteForDifferentDirectInvitee() {
		queryRepository.invite = Optional.of(invite(UUID.randomUUID(), InviteStatus.PENDING, null));

		assertThatThrownBy(() -> handler.handle(new AcceptTripInviteCommand("ABCD1234", actorUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	@Test
	void rejectsAlreadyAcceptedInvite() {
		queryRepository.invite = Optional.of(invite(null, InviteStatus.ACCEPTED, null));

		assertThatThrownBy(() -> handler.handle(new AcceptTripInviteCommand("ABCD1234", actorUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
			);
	}

	@Test
	void rejectsExpiredInvite() {
		queryRepository.invite = Optional.of(invite(
			null,
			InviteStatus.PENDING,
			Instant.parse("2026-06-15T00:00:00Z")
		));

		assertThatThrownBy(() -> handler.handle(new AcceptTripInviteCommand("ABCD1234", actorUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
			);
	}

	@Test
	void rejectsAlreadyActiveMember() {
		queryRepository.invite = Optional.of(invite(null, InviteStatus.PENDING, null));
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			actorUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new AcceptTripInviteCommand("ABCD1234", actorUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT)
			);
	}

	private TripInviteAcceptReadModel invite(UUID inviteeUserId, InviteStatus status, Instant expiresAt) {
		return new TripInviteAcceptReadModel(
			inviteId,
			tripId,
			"ABCD1234",
			inviteeUserId,
			status,
			expiresAt,
			ownerUserId,
			TripStatus.ACTIVE
		);
	}

	private TimeProvider fixedTime() {
		return () -> Instant.parse("2026-06-16T00:00:00Z");
	}

	private static class CapturingTripCommandRepository implements TripCommandRepository {

		private TripMember addedMember;
		private UUID acceptedInviteId;
		private UUID acceptedByUserId;

		@Override
		public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
		}

		@Override
		public void saveTripInvite(TripInvite invite) {
		}

		@Override
		public void revokeTripInvite(UUID inviteId, UUID revokedByUserId, Instant revokedAt) {
		}

		@Override
		public void addTripMember(TripMember member) {
			this.addedMember = member;
		}

		@Override
		public void acceptTripInvite(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt) {
			this.acceptedInviteId = inviteId;
			this.acceptedByUserId = acceptedByUserId;
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

	private static class StubTripQueryRepository implements TripQueryRepository {

		private Optional<TripInviteAcceptReadModel> invite = Optional.empty();
		private Optional<TripAccessSnapshot> access = Optional.empty();

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return access;
		}

		@Override
		public Optional<com.soomgil.trip.application.port.TripReadModel> findTrip(UUID tripId) {
			return Optional.empty();
		}

		@Override
		public List<com.soomgil.trip.application.port.TripMemberReadModel> findTripMembers(
			UUID tripId,
			TripMemberStatus status
		) {
			return List.of();
		}

		@Override
		public com.soomgil.trip.application.port.TripSummaryPage findMyTrips(
			UUID userId,
			TripStatus status,
			TripAccessRole role,
			int page,
			int size,
			List<String> sort
		) {
			return new com.soomgil.trip.application.port.TripSummaryPage(List.of(), 0);
		}

		@Override
		public List<TripInviteReadModel> findTripInvites(UUID tripId, InviteStatus status) {
			return List.of();
		}

		@Override
		public Optional<TripInviteAcceptReadModel> findTripInviteForAccept(String inviteCode) {
			return invite;
		}
	}
}
