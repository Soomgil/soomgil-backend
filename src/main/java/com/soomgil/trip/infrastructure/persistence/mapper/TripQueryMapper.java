package com.soomgil.trip.infrastructure.persistence.mapper;

import com.soomgil.trip.infrastructure.persistence.row.TripAccessRow;
import com.soomgil.trip.infrastructure.persistence.row.TripMemberReadRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import java.util.List;
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

	/**
	 * 여행방 기본 정보를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 여행방이 없으면 null
	 */
	TripRow findTrip(@Param("tripId") UUID tripId);

	/**
	 * 여행방 멤버 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param status 선택적 멤버 상태
	 * @return 멤버 목록
	 */
	List<TripMemberReadRow> findTripMembers(@Param("tripId") UUID tripId, @Param("status") String status);

	/**
	 * 현재 사용자가 active member인 여행방 목록을 조회한다.
	 *
	 * @param userId 사용자 ID
	 * @param status 선택적 여행방 상태
	 * @param role 선택적 파생 access role
	 * @param size page 크기
	 * @param offset 시작 offset
	 * @return 여행방 목록
	 */
	List<TripRow> findMyTrips(
		@Param("userId") UUID userId,
		@Param("status") String status,
		@Param("role") String role,
		@Param("size") int size,
		@Param("offset") int offset
	);

	/**
	 * 현재 사용자가 active member인 여행방 개수를 조회한다.
	 *
	 * @param userId 사용자 ID
	 * @param status 선택적 여행방 상태
	 * @param role 선택적 파생 access role
	 * @return 전체 개수
	 */
	long countMyTrips(@Param("userId") UUID userId, @Param("status") String status, @Param("role") String role);
}
