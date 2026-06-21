package com.soomgil.user.application.service;

import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripStatus;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 계정 삭제 전에 사용자가 소유한 활성 여행방의 존재 여부를 조회한다. */
@Component
public class TripActiveOwnerTripChecker implements ActiveOwnerTripChecker {

	private final TripQueryRepository tripQueryRepository;

	public TripActiveOwnerTripChecker(TripQueryRepository tripQueryRepository) {
		this.tripQueryRepository = Objects.requireNonNull(tripQueryRepository, "tripQueryRepository must not be null");
	}

	@Override
	public boolean hasActiveOwnerTrip(UUID userId) {
		return tripQueryRepository.findMyTrips(
			userId, TripStatus.ACTIVE, TripAccessRole.OWNER, 0, 1, List.of()
		).totalElements() > 0;
	}
}
