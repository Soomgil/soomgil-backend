package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.net.URI;
import java.util.UUID;

/**
 * itinerary item 조회 모델.
 *
 * @param id item ID
 * @param itineraryDayId day ID
 * @param sortOrder 정렬 순서
 * @param itemType item 타입
 * @param placeProvider 장소 provider
 * @param externalPlaceId 외부 장소 ID
 * @param placeName 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 썸네일 URL
 * @param sourceStatus 장소 source 상태
 */
public record ItineraryItemReadModel(
	UUID id,
	UUID itineraryDayId,
	Integer sortOrder,
	ItineraryItemType itemType,
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String sourceStatus
) {
}
