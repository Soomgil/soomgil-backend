package com.soomgil.planning.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripMemberAccessCheckerAdapterTest {

	@Test
	void delegatesMembershipValidationToTripAccessGuard() {
		TripAccessGuard guard = mock(TripAccessGuard.class);
		TripMemberAccessChecker checker = new TripMemberAccessCheckerAdapter(guard);
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		checker.requireMember(tripId, userId);

		verify(guard).requireActiveMember(tripId, userId);
	}
}
