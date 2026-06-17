package com.soomgil.itinerary.application.command.dto;

import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 일정 변경 응답에 포함되는 day view.
 */
public record ItineraryDayView(
	UUID id,
	UUID tripId,
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	int sortOrder
) {
}
