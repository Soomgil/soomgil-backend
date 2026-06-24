package com.soomgil.record.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행 기록 entry 조회 모델.
 */
public record TripRecordEntryReadModel(
	UUID id,
	UUID tripId,
	String tripTitle,
	UUID itineraryDayId,
	UUID itineraryItemId,
	UUID uploadedByUserId,
	String uploadedByDisplayName,
	String uploadedByProfileImageUrl,
	String title,
	String caption,
	String locationName,
	Double lat,
	Double lng,
	OffsetDateTime takenAt,
	String visibility,
	String status,
	OffsetDateTime createdAt
) {
}
