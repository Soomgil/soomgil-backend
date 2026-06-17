package com.soomgil.itinerary.application.command.dto;

import java.util.UUID;

/**
 * 재정렬 snapshot 안의 item day 이동 및 순서 변경 값.
 */
public record ItineraryItemOrderCommand(
	UUID itemId,
	int sortOrder
) {
}
