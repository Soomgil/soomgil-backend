package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.net.URI;
import java.util.UUID;

/**
 * itinerary item 수정 command.
 *
 * @param tripId 여행방 ID
 * @param actorUserId 요청 사용자 ID
 * @param baseVersion 요청자가 본 itinerary version
 * @param itemId 수정할 item ID
 * @param itineraryDayId 이동할 day ID
 * @param sortOrder 정렬 순서
 * @param placeName 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 썸네일 URL
 */
public record UpdateItineraryItemCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	UUID itemId,
	UUID itineraryDayId,
	Integer sortOrder,
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl
) implements Command<ItineraryMutationResult> {
}
