package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 스와이프 feed 장소 조회 row.
 */
public record SwipeFeedPlaceRow(
	Integer contentId,
	String title,
	String address,
	Double latitude,
	Double longitude,
	String thumbnailUrl,
	String category,
	String myReaction
) {
}
