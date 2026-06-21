package com.soomgil.trip.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.UpdateTripCommand;
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

class UpdateTripHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID memberUserId = UUID.randomUUID();
	private final CapturingTripCommandRepository commandRepository = new CapturingTripCommandRepository();
	private final StubTripQueryRepository queryRepository = new StubTripQueryRepository();
	private final UpdateTripHandler handler = new UpdateTripHandler(
		commandRepository,
		queryRepository,
		fixedTime()
	);

	@Test
	void ownerCanUpdateTripSettings() {
		queryRepository.access = ownerAccess();

		handler.handle(new UpdateTripCommand(
			tripId,
			ownerUserId,
			"  제주 여행  ",
			"  제주  ",
			List.of("5011010100", "5011010100", "5011010200"),
			TripStatus.ARCHIVED
		));

		assertThat(commandRepository.updated.tripId()).isEqualTo(tripId);
		assertThat(commandRepository.updated.title()).isEqualTo("제주 여행");
		assertThat(commandRepository.updated.displayDestinationProvided()).isTrue();
		assertThat(commandRepository.updated.displayDestination()).isEqualTo("제주");
		assertThat(commandRepository.updated.status()).isEqualTo(TripStatus.ARCHIVED);
		assertThat(commandRepository.updated.updatedAt()).isEqualTo(now());
		assertThat(commandRepository.replacedRegionCodes).containsExactly("5011010100", "5011010200");
	}

	@Test
	void ownerCanClearDisplayDestinationAndRegions() {
		queryRepository.access = ownerAccess();

		handler.handle(new UpdateTripCommand(
			tripId,
			ownerUserId,
			null,
			"  ",
			List.of(),
			null
		));

		assertThat(commandRepository.updated.title()).isNull();
		assertThat(commandRepository.updated.displayDestinationProvided()).isTrue();
		assertThat(commandRepository.updated.displayDestination()).isNull();
		assertThat(commandRepository.replacedRegionCodes).isEmpty();
	}

	@Test
	void regionOnlyUpdateTouchesTripUpdatedAt() {
		queryRepository.access = ownerAccess();

		handler.handle(new UpdateTripCommand(
			tripId,
			ownerUserId,
			null,
			null,
			List.of("5011010100"),
			null
		));

		assertThat(commandRepository.updated.updatedAt()).isEqualTo(now());
		assertThat(commandRepository.replacedRegionCodes).containsExactly("5011010100");
	}

	@Test
	void nonOwnerCannotUpdateTripSettings() {
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new UpdateTripCommand(
			tripId,
			memberUserId,
			"제주 여행",
			null,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
		);
	}

	@Test
	void patchCannotSetDeletedStatus() {
		queryRepository.access = ownerAccess();

		assertThatThrownBy(() -> handler.handle(new UpdateTripCommand(
			tripId,
			ownerUserId,
			null,
			null,
			null,
			TripStatus.DELETED
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED)
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

	private static class CapturingTripCommandRepository implements TripCommandRepository {

		private TripSettingsUpdate updated;
		private List<String> replacedRegionCodes;

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
			this.updated = update;
		}

		@Override
		public void replaceTripRegions(UUID tripId, List<String> legalRegionCodes, Instant createdAt) {
			this.replacedRegionCodes = legalRegionCodes;
		}

		@Override
		public void softDeleteTrip(UUID tripId, Instant deletedAt) {
		}

		@Override
		public void removeTripMember(UUID tripId, UUID userId, UUID removedByUserId, Instant removedAt) {
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
