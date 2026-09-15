package com.soomgil.itinerary.application.command.dto;

import java.util.UUID;

/**
 * 이미 일정에 있어 추가하지 않은 장소.
 *
 * <p>{@code existingItineraryItemId}는 실제 day와 일차 미정을 가리지 않고 이미 존재하던 item이다.
 * 호출자는 이 목록을 실패가 아니라 정상적인 중복 방지 결과로 취급해야 한다.
 *
 * @param placeProvider 장소 원천 provider
 * @param externalPlaceId 외부 장소 id
 * @param placeName 장소명
 * @param existingItineraryItemId 이미 존재하던 일정 item 식별자
 */
public record SkippedUnscheduledPlace(
	String placeProvider,
	String externalPlaceId,
	String placeName,
	UUID existingItineraryItemId
) {
}
