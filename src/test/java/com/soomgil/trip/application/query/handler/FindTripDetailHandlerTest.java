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
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindTripDetailHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID memberUserId = UUID.randomUUID();
	private final StubTripQueryRepository repository = new StubTripQueryRepository();
	private final FindTripDetailHandler handler = new FindTripDetailHandler(new TripAccessGuard(repository), repository);

	@Test
	void activeMemberCanFindTripDetailWithDerivedOwnerMemberRole() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			ownerUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));
		repository.trip = Optional.of(trip(ownerUserId));
		repository.members = List.of(
			member(ownerUserId, ownerUserId),
			member(memberUserId, ownerUserId)
		);

		TripDetailView view = handler.handle(new FindTripDetailQuery(tripId, ownerUserId));

		assertThat(view.id()).isEqualTo(tripId);
		assertThat(view.ownerUserId()).isEqualTo(ownerUserId);
		assertThat(view.myRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(view.members()).hasSize(2);
		assertThat(view.members().get(0).accessRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(view.members().get(1).accessRole()).isEqualTo(TripAccessRole.MEMBER);
	}

	@Test
	void nonMemberCannotFindTripDetail() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			null,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new FindTripDetailQuery(tripId, memberUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	@Test
	void deletedTripIsNotFound() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			ownerUserId,
			TripStatus.DELETED,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new FindTripDetailQuery(tripId, ownerUserId)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND)
			);
	}

	private TripReadModel trip(UUID ownerUserId) {
		return new TripReadModel(
			tripId,
			ownerUserId,
			"제주 숨길",
			"제주",
			TripStatus.ACTIVE,
			0,
			Instant.parse("2026-06-16T00:00:00Z"),
			null
		);
	}

	private TripMemberReadModel member(UUID userId, UUID ownerUserId) {
		return new TripMemberReadModel(
			UUID.randomUUID(),
			tripId,
			userId,
			TripMemberRole.MEMBER,
			TripMemberStatus.ACTIVE,
			Instant.parse("2026-06-16T00:00:00Z"),
			ownerUserId
		);
	}

	private static class StubTripQueryRepository implements TripQueryRepository {

		private Optional<TripAccessSnapshot> access = Optional.empty();
		private Optional<TripReadModel> trip = Optional.empty();
		private List<TripMemberReadModel> members = List.of();

		@Override
		public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
			return access;
		}

		@Override
		public Optional<TripReadModel> findTrip(UUID tripId) {
			return trip;
		}

		@Override
		public List<TripMemberReadModel> findTripMembers(UUID tripId, TripMemberStatus status) {
			return members;
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

		@Override
		public Optional<com.soomgil.trip.application.port.TripInviteAcceptReadModel> findTripInviteForAccept(
			String inviteCode
		) {
			return Optional.empty();
		}
	}
}
