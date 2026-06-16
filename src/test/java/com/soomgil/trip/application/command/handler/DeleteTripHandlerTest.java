package com.soomgil.trip.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.DeleteTripCommand;
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

class DeleteTripHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID memberUserId = UUID.randomUUID();
	private final CapturingTripCommandRepository commandRepository = new CapturingTripCommandRepository();
	private final StubTripQueryRepository queryRepository = new StubTripQueryRepository();
	private final DeleteTripHandler handler = new DeleteTripHandler(
		commandRepository,
		queryRepository,
		fixedTime()
	);

	@Test
	void ownerCanSoftDeleteTrip() {
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			ownerUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		handler.handle(new DeleteTripCommand(tripId, ownerUserId));

		assertThat(commandRepository.deletedTripId).isEqualTo(tripId);
		assertThat(commandRepository.deletedAt).isEqualTo(now());
	}

	@Test
	void nonOwnerCannotSoftDeleteTrip() {
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new DeleteTripCommand(tripId, memberUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	private Instant now() {
		return Instant.parse("2026-06-16T00:00:00Z");
	}

	private TimeProvider fixedTime() {
		return this::now;
	}

	private static class CapturingTripCommandRepository implements TripCommandRepository {

		private UUID deletedTripId;
		private Instant deletedAt;

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
		}

		@Override
		public void acceptTripInvite(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt) {
		}

		@Override
		public void updateTrip(TripSettingsUpdate update) {
		}

		@Override
		public void replaceTripRegions(UUID tripId, List<String> legalRegionCodes, Instant createdAt) {
		}

		@Override
		public void softDeleteTrip(UUID tripId, Instant deletedAt) {
			this.deletedTripId = tripId;
			this.deletedAt = deletedAt;
		}
	}

	private static class StubTripQueryRepository implements TripQueryRepository {

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
		public Optional<com.soomgil.trip.application.port.TripInviteAcceptReadModel> findTripInviteForAccept(
			String inviteCode
		) {
			return Optional.empty();
		}
	}
}
