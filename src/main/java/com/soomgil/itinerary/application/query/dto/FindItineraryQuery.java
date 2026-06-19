package com.soomgil.itinerary.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.UUID;

/**
 * 여행방 itinerary 전체 조회 query.
 *
 * @param tripId 여행방 ID
 * @param userId 요청 사용자 ID
 */
public record FindItineraryQuery(
	UUID tripId,
	UUID userId
) implements Query<ItineraryView> {
}
