package com.soomgil.itinerary.application.command.dto;

import java.util.List;
import java.util.UUID;

/**
 * 재정렬 snapshot 안의 day 순서 변경 값.
 */
public record ItineraryDayOrderCommand(
	UUID dayId,
	int sortOrder,
	List<ItineraryItemOrderCommand> itemOrders
) {

	public ItineraryDayOrderCommand {
		itemOrders = itemOrders == null ? List.of() : List.copyOf(itemOrders);
	}
}
