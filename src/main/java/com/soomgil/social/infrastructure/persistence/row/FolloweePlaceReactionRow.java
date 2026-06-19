package com.soomgil.social.infrastructure.persistence.row;

/**
 * 팔로우 사용자의 장소별 긍정 반응 조회 row.
 */
public record FolloweePlaceReactionRow(
	String provider,
	String externalPlaceId,
	String userId,
	String displayName,
	String profileImageUrl
) {
}
