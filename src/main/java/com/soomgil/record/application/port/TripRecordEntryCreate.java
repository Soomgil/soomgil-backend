package com.soomgil.record.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행 기록 entry 생성 모델.
 */
public record TripRecordEntryCreate(
	UUID id,
	UUID tripId,
	UUID itineraryDayId,
	UUID itineraryItemId,
	UUID uploadedByUserId,
	String title,
	String caption,
	String locationName,
	Double lat,
	Double lng,
	OffsetDateTime takenAt,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {
}
