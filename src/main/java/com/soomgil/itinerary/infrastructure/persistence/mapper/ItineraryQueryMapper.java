package com.soomgil.itinerary.infrastructure.persistence.mapper;

import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.infrastructure.persistence.row.ItineraryItemRow;
import com.soomgil.itinerary.infrastructure.persistence.row.MapDrawingRow;
import com.soomgil.itinerary.infrastructure.persistence.row.RouteSegmentRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * itinerary 읽기 SQL mapper.
 */
@Mapper
public interface ItineraryQueryMapper {

	/**
	 * 여행방 itinerary version을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return 여행방이 없으면 null
	 */
	Long findItineraryVersion(@Param("tripId") UUID tripId);

	/**
	 * 여행방 day 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return day 목록
	 */
	List<ItineraryDayReadModel> findDays(@Param("tripId") UUID tripId);

	/**
	 * 여행방 active item 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return item row 목록
	 */
	List<ItineraryItemRow> findItems(@Param("tripId") UUID tripId);

	/**
	 * 여행방 active route 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return route row 목록
	 */
	List<RouteSegmentRow> findRoutes(@Param("tripId") UUID tripId);

	/**
	 * 여행방 active drawing 목록을 조회한다.
	 *
	 * @param tripId 여행방 ID
	 * @return drawing row 목록
	 */
	List<MapDrawingRow> findMapDrawings(@Param("tripId") UUID tripId);
}
