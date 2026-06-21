package com.soomgil.trip.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.RemoveTripMemberCommand;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
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

class RemoveTripMemberHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID memberUserId = UUID.randomUUID();
	private final CapturingTripCommandRepository commandRepository = new CapturingTripCommandRepository();
	private final StubTripQueryRepository queryRepository = new StubTripQueryRepository();
	private final RemoveTripMemberHandler handler = new RemoveTripMemberHandler(
		commandRepository,
		queryRepository,
		fixedTime()
	);

	@Test
	void ownerCanRemoveActiveMember() {
		queryRepository.ownerAccess = ownerAccess();
		queryRepository.targetAccess = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		handler.handle(new RemoveTripMemberCommand(tripId, memberUserId, ownerUserId));

		assertThat(commandRepository.removedTripId).isEqualTo(tripId);
		assertThat(commandRepository.removedUserId).isEqualTo(memberUserId);
		assertThat(commandRepository.removedByUserId).isEqualTo(ownerUserId);
		assertThat(commandRepository.removedAt).isEqualTo(now());
	}

	@Test
	void nonOwnerCannotRemoveMember() {
		queryRepository.targetAccess = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new RemoveTripMemberCommand(tripId, ownerUserId, memberUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	@Test
	void ownerCannotRemoveSelf() {
		queryRepository.ownerAccess = ownerAccess();

		assertThatThrownBy(() -> handler.handle(new RemoveTripMemberCommand(tripId, ownerUserId, ownerUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION)
			);
	}

	@Test
	void cannotRemoveInactiveMember() {
		queryRepository.ownerAccess = ownerAccess();
		queryRepository.targetAccess = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.REMOVED,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new RemoveTripMemberCommand(tripId, memberUserId, ownerUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
			);
	}

	private Optional<TripAccessSnapshot> ownerAccess() {
		return Optional.of(new TripAccessSnapshot(
			tripId,
			ownerUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));
	}

	private Instant now() {
		return Instant.parse("2026-06-16T00:00:00Z");
	}

	private TimeProvider fixedTime() {
		return this::now;
	}

	private class CapturingTripCommandRepository implements TripCommandRepository {

		private UUID removedTripId;
		private UUID removedUserId;
		private UUID removedByUserId;
		private Instant removedAt;

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
		}

		@Override
		public boolean acceptTripInvite(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt) {
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
			this.removedTripId = tripId;
			this.removedUserId = userId;
			this.removedByUserId = removedByUserId;
			this.removedAt = removedAt;
		}
	}

	private class StubTripQueryRepository implements TripQueryRepository {

		private Optional<TripAccessSnapshot> ownerAccess = Optional.empty();
		private Optional<TripAccessSnapshot> targetAccess = Optional.empty();

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return userId.equals(ownerUserId) ? ownerAccess : targetAccess;
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
		public Optional<com.soomgil.trip.application.port.TripInviteAcceptReadModel> findTripInviteForAccept(
			String inviteCode
		) {
			return Optional.empty();
		}
	}
}
