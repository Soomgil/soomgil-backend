package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.util.UUID;

/**
 * itinerary.itinerary_items 수정 모델.
 *
 * @param tripId 여행방 ID
 * @param itemId item ID
 * @param itineraryDayId 변경할 day ID
 * @param sortOrder 변경할 정렬 순서
 * @param placeName 변경할 장소명
 * @param address 변경할 주소
 * @param lat 변경할 위도
 * @param lng 변경할 경도
 * @param thumbnailUrl 변경할 썸네일 URL
 * @param updatedByUserId 수정 사용자 ID
 * @param updatedAt 수정 시각
 */
public record ItineraryItemUpdate(
	UUID tripId,
	UUID itemId,
	UUID itineraryDayId,
	Integer sortOrder,
	String placeName,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	UUID updatedByUserId,
	Instant updatedAt
) {
}
