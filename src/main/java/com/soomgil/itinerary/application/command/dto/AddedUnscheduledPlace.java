package com.soomgil.itinerary.application.command.dto;

import java.util.UUID;

/**
 * 일차 미정에 실제로 추가된 장소.
 *
 * @param itineraryItemId 새로 생성된 일정 item 식별자
 * @param placeProvider 장소 원천 provider
 * @param externalPlaceId 외부 장소 id
 * @param placeName 장소명
 */
public record AddedUnscheduledPlace(
	UUID itineraryItemId,
	String placeProvider,
	String externalPlaceId,
	String placeName
) {
}
