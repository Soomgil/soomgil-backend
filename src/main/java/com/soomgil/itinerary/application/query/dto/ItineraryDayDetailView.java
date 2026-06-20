package com.soomgil.itinerary.application.query.dto;

import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * item 목록을 포함한 itinerary day 조회 view.
 *
 * @param id day ID
 * @param tripId 여행방 ID
 * @param groupType day 그룹 타입
 * @param dayNumber 일차 번호
 * @param date 일정 날짜
 * @param title day 제목
 * @param sortOrder 정렬 순서
 * @param items day에 속한 item 목록
 */
public record ItineraryDayDetailView(
	UUID id,
	UUID tripId,
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	Integer sortOrder,
	List<ItineraryItemView> items
) {
}
