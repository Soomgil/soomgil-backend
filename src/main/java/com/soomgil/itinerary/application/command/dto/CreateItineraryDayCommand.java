package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 여행방 일정 day 생성을 요청하는 command.
 *
 * <p>호출자는 active trip member여야 하며 {@code baseVersion}은 현재 trip itinerary version과 같아야 한다.
 */
public record CreateItineraryDayCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	ItineraryDayGroupType groupType,
	Integer dayNumber,
	LocalDate date,
	String title,
	Integer sortOrder
) implements Command<ItineraryMutationResult> {
}
