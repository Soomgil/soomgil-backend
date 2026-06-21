package com.soomgil.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripActiveOwnerTripCheckerTest {

	@Test
	void reportsWhetherTheUserOwnsAnyActiveTrip() {
		TripQueryRepository repository = mock(TripQueryRepository.class);
		UUID userId = UUID.randomUUID();
		when(repository.findMyTrips(userId, TripStatus.ACTIVE, TripAccessRole.OWNER, 0, 1, List.of()))
			.thenReturn(new TripSummaryPage(List.of(), 1));
		ActiveOwnerTripChecker checker = new TripActiveOwnerTripChecker(repository);

		assertThat(checker.hasActiveOwnerTrip(userId)).isTrue();
		verify(repository).findMyTrips(userId, TripStatus.ACTIVE, TripAccessRole.OWNER, 0, 1, List.of());
	}
}
