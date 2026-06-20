package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.time.LocalDate;
import java.util.UUID;

/**
 * itinerary day 수정 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param dayId 수정할 day ID
 * @param dayNumber 일차 번호
 * @param date 일정 날짜
 * @param title day 제목
 * @param sortOrder 정렬 순서
 */
public record UpdateItineraryDayCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID dayId,
	Integer dayNumber,
	LocalDate date,
	String title,
	Integer sortOrder
) implements Command<ItineraryMutationResult> {
}
