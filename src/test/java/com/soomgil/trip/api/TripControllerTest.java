package com.soomgil.trip.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.api.dto.CreateTripRequest;
import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripDetail;
import com.soomgil.trip.api.dto.TripStatus;
import com.soomgil.trip.application.command.handler.CreateTripHandler;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripMember;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripControllerTest {

	@Test
	void createTripReturnsOwnerTripDetail() {
		UUID currentUserId = UUID.randomUUID();
		TripController controller = new TripController(new CreateTripHandler(new NoopTripCommandRepository(), fixedTime()));
		CreateTripRequest request = new CreateTripRequest("서울 여행", "서울", List.of(), null, null);

		TripDetail detail = controller.createTrip(request, principal(currentUserId));

		assertThat(detail.id()).isNotNull();
		assertThat(detail.ownerUserId()).isEqualTo(currentUserId);
		assertThat(detail.title()).isEqualTo("서울 여행");
		assertThat(detail.displayDestination()).isEqualTo("서울");
		assertThat(detail.status()).isEqualTo(TripStatus.ACTIVE);
		assertThat(detail.myRole()).isEqualTo(TripAccessRole.OWNER);
		assertThat(detail.itineraryVersion()).isZero();
		assertThat(detail.members()).isEmpty();
		assertThat(detail.regions()).isEmpty();
	}

	private static Principal principal(UUID userId) {
		return userId::toString;
	}

	private static TimeProvider fixedTime() {
		return () -> Instant.parse("2026-06-16T00:00:00Z");
	}

	private static class NoopTripCommandRepository implements TripCommandRepository {

		@Override
		public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
		}
	}
}
