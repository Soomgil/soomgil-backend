package com.soomgil.itinerary.domain.model;

/**
 * 일정 item 유형.
 *
 * <p>{@code PLACE}는 외부 장소 참조가 필요하고, {@code CUSTOM_PLACE}는 item 자체 표시 정보만 사용한다.
 */
public enum ItineraryItemType {
	PLACE,
	CUSTOM_PLACE
}
