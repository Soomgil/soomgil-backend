package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import java.util.List;

/**
 * 스와이프 카드에 필요한 관광공사 장소 정보와 내부 태그.
 */
public record SwipeFeedPlace(
	PlaceProvider provider,
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String category,
	PlaceSourceStatus sourceStatus,
	String description,
	List<String> photos,
	List<String> tags
) {
	public SwipeFeedPlace {
		photos = photos == null ? List.of() : List.copyOf(photos);
		tags = tags == null ? List.of() : List.copyOf(tags);
	}
}
