package com.soomgil.itinerary.infrastructure.persistence.mapper;

import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
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
	 * trip itinerary version을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 여행방이 없으면 null
	 */
	Long findItineraryVersion(@Param("tripId") UUID tripId);

	/**
	 * 일정 day를 추가한다.
	 *
	 * @param day 저장할 day
	 */
	void insertDay(@Param("day") ItineraryDayCreate day);

	/**
	 * 일차 미정 day를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 존재하지 않으면 null
	 */
	ItineraryDayReadModel findUnscheduledDay(@Param("tripId") UUID tripId);

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

	/**
	 * 여행방 itinerary day 수를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return day 수
	 */
	long countDays(@Param("tripId") UUID tripId);

	/**
	 * 일정 item 존재 여부를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @param itemId 일정 item ID
	 * @return 존재 여부
	 */
	boolean existsItem(@Param("tripId") UUID tripId, @Param("itemId") UUID itemId);

	/**
	 * 삭제되지 않은 일정 item 수를 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return active item 수
	 */
	long countActiveItems(@Param("tripId") UUID tripId);

	/**
	 * 일정 day sort order를 갱신한다.
	 *
	 * @param update 갱신할 day 순서
	 */
	void updateDayOrder(@Param("update") ItineraryDayOrderUpdate update);

	/**
	 * 일정 item day 소속과 sort order를 갱신한다.
	 *
	 * @param update 갱신할 item 순서
	 */
	void updateItemOrder(@Param("update") ItineraryItemOrderUpdate update);
}
