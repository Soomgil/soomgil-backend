package com.soomgil.trip.infrastructure.persistence.mapper;

import com.soomgil.trip.infrastructure.persistence.row.TripMemberRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRegionRow;
import com.soomgil.trip.infrastructure.persistence.row.TripInviteRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 여행방 쓰기 SQL mapper.
 *
 * <p>transaction 경계는 command handler가 가진다. mapper는 단일 SQL 실행만 담당한다.
 */
@Mapper
public interface TripCommandMapper {

	/**
	 * 여행방 row를 추가한다.
	 *
	 * @param row 저장할 여행방 row
	 */
	void insertTrip(TripRow row);

	/**
	 * 여행방 멤버십 row를 추가한다.
	 *
	 * @param row 저장할 멤버십 row
	 */
	void insertTripMember(TripMemberRow row);

	/**
	 * 여행방 법정동 code row를 추가한다.
	 *
	 * @param row 저장할 지역 row
	 */
	void insertTripRegion(TripRegionRow row);

	/**
	 * 여행방 초대 row를 추가한다.
	 *
	 * @param row 저장할 초대 row
	 */
	void insertTripInvite(TripInviteRow row);

	/**
	 * 대기 중인 여행방 초대를 취소 상태로 전환한다.
	 *
	 * @param inviteId 초대 ID
	 * @param revokedAt 취소 시각
	 */
	void revokeTripInvite(@Param("inviteId") UUID inviteId, @Param("revokedAt") Instant revokedAt);

	/**
	 * 초대를 수락 상태로 전환한다.
	 *
	 * @param inviteId 초대 ID
	 * @param acceptedByUserId 수락한 사용자 ID
	 * @param acceptedAt 수락 시각
	 */
	void acceptTripInvite(
		@Param("inviteId") UUID inviteId,
		@Param("acceptedByUserId") UUID acceptedByUserId,
		@Param("acceptedAt") Instant acceptedAt
	);
}
