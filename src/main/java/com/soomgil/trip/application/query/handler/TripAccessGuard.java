package com.soomgil.trip.application.query.handler;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.trip.domain.policy.TripAccessPolicy;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 여행방 active member 권한을 요구하는 application guard.
 *
 * <p>없는 여행방과 DELETED 여행방은 404로, active member가 아닌 사용자는 403으로 변환한다.
 */
@Component
public class TripAccessGuard {

	private final TripQueryRepository repository;

	public TripAccessGuard(TripQueryRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	/**
	 * 여행방 active member 접근 권한을 요구한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 요청 사용자 ID
	 * @return 접근 가능 view
	 */
	public TripAccessView requireActiveMember(UUID tripId, UUID userId) {
		TripAccessSnapshot snapshot = repository.findTripAccess(tripId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found."));
		if (snapshot.tripStatus() == TripStatus.DELETED) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found.");
		}

		TripAccessView access = TripAccessPolicy.evaluate(tripId, userId, snapshot);
		if (!access.canAccess()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Trip member access is required.");
		}
		return access;
	}
}
