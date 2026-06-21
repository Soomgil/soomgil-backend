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
	List<String> tags,
	TagPreparationStatus tagStatus
) {
	public SwipeFeedPlace {
		photos = photos == null ? List.of() : List.copyOf(photos);
		tags = tags == null ? List.of() : List.copyOf(tags);
		tagStatus = tagStatus == null ? TagPreparationStatus.READY : tagStatus;
	}

	public SwipeFeedPlace(
		PlaceProvider provider, String externalPlaceId, String name, String address, Double lat, Double lng,
		String thumbnailUrl, String category, PlaceSourceStatus sourceStatus, String description,
		List<String> photos, List<String> tags
	) {
		this(provider, externalPlaceId, name, address, lat, lng, thumbnailUrl, category, sourceStatus,
			description, photos, tags, TagPreparationStatus.READY);
	}
}
