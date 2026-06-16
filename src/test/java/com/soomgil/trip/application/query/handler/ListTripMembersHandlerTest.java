package com.soomgil.trip.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListTripMembersHandlerTest {

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerUserId = UUID.randomUUID();
	private final UUID memberUserId = UUID.randomUUID();
	private final StubTripQueryRepository repository = new StubTripQueryRepository();
	private final ListTripMembersHandler handler = new ListTripMembersHandler(new TripAccessGuard(repository), repository);

	@Test
	void activeMemberCanListTripMembers() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			TripMemberStatus.ACTIVE,
			ownerUserId
		));
		repository.members = List.of(member(ownerUserId), member(memberUserId));

		List<TripMemberView> members = handler.handle(new ListTripMembersQuery(
			tripId,
			memberUserId,
			TripMemberStatus.ACTIVE
		));

		assertThat(members).hasSize(2);
		assertThat(members.get(0).accessRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(members.get(1).accessRole()).isEqualTo(TripAccessRole.MEMBER);
	}

	@Test
	void nonMemberCannotListTripMembers() {
		repository.access = Optional.of(new TripAccessSnapshot(
			tripId,
			memberUserId,
			TripStatus.ACTIVE,
			null,
			ownerUserId
		));

		assertThatThrownBy(() -> handler.handle(new ListTripMembersQuery(tripId, memberUserId, null)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN)
			);
	}

	private TripMemberReadModel member(UUID userId) {
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
		private List<TripMemberReadModel> members = List.of();

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
	}
}
