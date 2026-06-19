package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.net.URI;
import java.util.UUID;

/**
 * 여행방 일정 item 생성을 요청하는 command.
 *
 * <p>item은 같은 trip의 기존 itinerary day에만 추가할 수 있다.
 */
public record CreateItineraryItemCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID itineraryDayId,
	int sortOrder,
	ItineraryItemType itemType,
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl
) implements Command<ItineraryMutationResult> {
}
