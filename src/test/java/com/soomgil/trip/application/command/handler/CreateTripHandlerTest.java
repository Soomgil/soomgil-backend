package com.soomgil.trip.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.trip.application.command.dto.CreateTripCommand;
import com.soomgil.trip.application.command.dto.CreateTripResult;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateTripHandlerTest {

	private final Instant now = Instant.parse("2026-06-16T00:00:00Z");
	private final CapturingTripCommandRepository repository = new CapturingTripCommandRepository();
	private final CreateTripHandler handler = new CreateTripHandler(repository, fixedTime());

	@Test
	void createsActiveTripAndInitialActiveMemberForCreator() {
		UUID creatorId = UUID.randomUUID();
		CreateTripCommand command = new CreateTripCommand(
			creatorId,
			"제주 숨길",
			"제주",
			List.of("5011000000")
		);

		CreateTripResult result = handler.handle(command);

		assertThat(result.tripId()).isNotNull();
		assertThat(result.ownerUserId()).isEqualTo(creatorId);
		assertThat(result.title()).isEqualTo("제주 숨길");
		assertThat(result.displayDestination()).isEqualTo("제주");
		assertThat(result.status()).isEqualTo(TripStatus.ACTIVE);
		assertThat(result.itineraryVersion()).isZero();
		assertThat(result.createdAt()).isEqualTo(now);
		assertThat(result.ownerMemberId()).isNotNull();

		assertThat(repository.savedTrip.ownerUserId()).isEqualTo(creatorId);
		assertThat(repository.savedTrip.status()).isEqualTo(TripStatus.ACTIVE);
		assertThat(repository.savedMember.tripId()).isEqualTo(result.tripId());
		assertThat(repository.savedMember.userId()).isEqualTo(creatorId);
		assertThat(repository.savedMember.role()).isEqualTo(TripMemberRole.MEMBER);
		assertThat(repository.savedMember.status()).isEqualTo(TripMemberStatus.ACTIVE);
		assertThat(repository.savedRegionCodes).containsExactly("5011000000");
	}

	@Test
	void trimsTitleAndDisplayDestinationBeforeSaving() {
		UUID creatorId = UUID.randomUUID();

		CreateTripResult result = handler.handle(new CreateTripCommand(
			creatorId,
			"  부산 여행  ",
			"  부산  ",
			List.of()
		));

		assertThat(result.title()).isEqualTo("부산 여행");
		assertThat(result.displayDestination()).isEqualTo("부산");
		assertThat(repository.savedTrip.title()).isEqualTo("부산 여행");
		assertThat(repository.savedTrip.displayDestination()).isEqualTo("부산");
	}

	@Test
	void rejectsBlankTitle() {
		assertThatThrownBy(() -> handler.handle(new CreateTripCommand(
			UUID.randomUUID(),
			" ",
			"제주",
			List.of()
		))).isInstanceOf(BusinessException.class);
	}

	private TimeProvider fixedTime() {
		return () -> now;
	}

	private static class CapturingTripCommandRepository implements TripCommandRepository {

		private Trip savedTrip;
		private TripMember savedMember;
		private List<String> savedRegionCodes;

		@Override
		public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
			this.savedTrip = trip;
			this.savedMember = initialMember;
			this.savedRegionCodes = List.copyOf(legalRegionCodes);
		}
	}
}
