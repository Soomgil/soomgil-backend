package com.soomgil.trip.infrastructure.persistence.mapper;

import com.soomgil.trip.infrastructure.persistence.row.TripAccessRow;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 여행방 읽기 SQL mapper.
 *
 * <p>다른 모듈이 사용할 access query에 필요한 최소 row만 조회한다.
 */
@Mapper
public interface TripQueryMapper {

	/**
	 * 여행방 접근 권한 계산에 필요한 trip/member 상태를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 사용자 ID
	 * @return 여행방이 없으면 null
	 */
	TripAccessRow findTripAccess(@Param("tripId") UUID tripId, @Param("userId") UUID userId);
}
