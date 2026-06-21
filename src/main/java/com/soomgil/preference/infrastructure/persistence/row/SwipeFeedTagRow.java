package com.soomgil.preference.infrastructure.persistence.row;

public record SwipeFeedTagRow(
	String externalPlaceId,
	String displayName,
	Integer rankOrder
) {
}
