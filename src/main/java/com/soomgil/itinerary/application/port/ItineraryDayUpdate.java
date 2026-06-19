package com.soomgil.itinerary.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * itinerary.itinerary_days 수정 모델.
 *
 * @param tripId 여행방 ID
 * @param dayId day ID
 * @param dayNumber 변경할 일차 번호
 * @param date 변경할 날짜
 * @param title 변경할 제목
 * @param sortOrder 변경할 정렬 순서
 * @param updatedAt 수정 시각
 */
public record ItineraryDayUpdate(
	UUID tripId,
	UUID dayId,
	Integer dayNumber,
	LocalDate date,
	String title,
	Integer sortOrder,
	Instant updatedAt
) {
}
