package com.soomgil.trip.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.CreateTripInviteCommand;
import com.soomgil.trip.application.command.dto.CreateTripInviteResult;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripInviteCodeGenerator;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripInviteNotificationPublisher;
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

class CreateTripInviteHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID inviteeUserId = UUID.randomUUID();
	private final CapturingTripCommandRepository commandRepository = new CapturingTripCommandRepository();
	private final StubTripQueryRepository queryRepository = new StubTripQueryRepository();
	private final TripInviteNotificationPublisher notificationPublisher = org.mockito.Mockito.mock(TripInviteNotificationPublisher.class);
	private final CreateTripInviteHandler handler = new CreateTripInviteHandler(
		commandRepository,
		queryRepository,
		fixedTime(),
		fixedCodeGenerator(),
		notificationPublisher
	);

	@Test
	void ownerCanCreatePendingInvite() {
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			ownerUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		CreateTripInviteResult result = handler.handle(new CreateTripInviteCommand(
			tripId,
			ownerUserId,
			inviteeUserId,
			null
		));

		assertThat(result.tripId()).isEqualTo(tripId);
		assertThat(result.inviteeUserId()).isEqualTo(inviteeUserId);
		assertThat(result.inviteCode()).isEqualTo("ABCD1234");
		assertThat(result.status()).isEqualTo(InviteStatus.PENDING);
		assertThat(result.createdAt()).isEqualTo(now());
		assertThat(result.expiresAt()).isNull();
		assertThat(commandRepository.savedInvite.createdByUserId()).isEqualTo(ownerUserId);
		assertThat(commandRepository.savedInvite.inviteTokenHash()).isNotBlank();
		org.mockito.Mockito.verify(notificationPublisher).publish(
			result.id(), tripId, ownerUserId, inviteeUserId, "ABCD1234", now()
		);
	}

	@Test
	void nonOwnerCannotCreateInvite() {
		queryRepository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			inviteeUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new CreateTripInviteCommand(
			tripId,
			inviteeUserId,
			null,
			null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
		);
	}

	private Instant now() {
		return Instant.parse("2026-06-16T00:00:00Z");
	}

	private TimeProvider fixedTime() {
		return this::now;
	}

	private TripInviteCodeGenerator fixedCodeGenerator() {
		return () -> "ABCD1234";
	}

	private static class CapturingTripCommandRepository implements TripCommandRepository {

		private TripInvite savedInvite;

		@Override
		public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
		}

		@Override
		public void saveTripInvite(TripInvite invite) {
			this.savedInvite = invite;
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
