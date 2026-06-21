package com.soomgil.planning.application.service;

import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Planning 요청의 여행방 멤버 권한을 trip 모듈의 단일 권한 정책으로 검증한다. */
@Component
public class TripMemberAccessCheckerAdapter implements TripMemberAccessChecker {

	private final TripAccessGuard tripAccessGuard;

	public TripMemberAccessCheckerAdapter(TripAccessGuard tripAccessGuard) {
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
	}

	@Override
	public void requireMember(UUID tripId, UUID userId) {
		tripAccessGuard.requireActiveMember(tripId, userId);
	}
}
