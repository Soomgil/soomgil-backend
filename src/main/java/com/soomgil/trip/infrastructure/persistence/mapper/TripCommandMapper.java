package com.soomgil.trip.infrastructure.persistence.mapper;

import com.soomgil.trip.infrastructure.persistence.row.TripMemberRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRegionRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import org.apache.ibatis.annotations.Mapper;

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
}
