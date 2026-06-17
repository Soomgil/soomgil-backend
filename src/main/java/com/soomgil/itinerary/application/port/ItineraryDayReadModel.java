package com.soomgil.itinerary.application.port;

import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * itinerary day 재사용 판단에 필요한 읽기 모델.
 */
public record ItineraryDayReadModel(
	UUID id,
	UUID tripId,
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	int sortOrder
) {
}
