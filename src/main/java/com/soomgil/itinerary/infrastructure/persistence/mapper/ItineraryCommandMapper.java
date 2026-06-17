package com.soomgil.itinerary.infrastructure.persistence.mapper;

import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 일정 day/item 쓰기 SQL mapper.
 */
@Mapper
public interface ItineraryCommandMapper {

	/**
	 * trip itinerary version을 조건부로 증가시킨다.
	 *
	 * @param tripId 여행방 ID
	 * @param baseVersion 요청 base version
	 * @param updatedAt 갱신 시각
	 * @return 증가된 version, 불일치하면 null
	 */
	Long incrementItineraryVersion(
		@Param("tripId") UUID tripId,
		@Param("baseVersion") long baseVersion,
		@Param("updatedAt") Instant updatedAt
	);

	/**
	 * 일정 day를 추가한다.
	 *
	 * @param day 저장할 day
	 */
	void insertDay(@Param("day") ItineraryDayCreate day);

	/**
	 * 일정 item을 추가한다.
	 *
	 * @param item 저장할 item
	 */
	void insertItem(@Param("item") ItineraryItemCreate item);

	/**
	 * 일정 day 존재 여부를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param dayId 일정 day ID
	 * @return 존재 여부
	 */
	boolean existsDay(@Param("tripId") UUID tripId, @Param("dayId") UUID dayId);
}
