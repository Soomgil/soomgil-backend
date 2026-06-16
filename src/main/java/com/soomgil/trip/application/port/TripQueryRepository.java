package com.soomgil.trip.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * 여행방 읽기 persistence 계약.
 *
 * <p>다른 모듈이 필요한 권한 판정용 데이터는 이 repository를 통해 조회하고,
 * application handler에서 접근 view로 변환한다.
 */
public interface TripQueryRepository {

	/**
	 * 특정 사용자와 여행방의 접근 판정에 필요한 snapshot을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 사용자 ID
	 * @return 여행방이 존재하면 snapshot, 없으면 empty
	 */
	Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId);
}
