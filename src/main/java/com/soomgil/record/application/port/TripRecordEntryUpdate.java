package com.soomgil.record.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행 기록 entry 수정 모델.
 */
public record TripRecordEntryUpdate(
	UUID tripId,
	UUID recordId,
	UUID itineraryDayId,
	UUID itineraryItemId,
	String title,
	String caption,
	String locationName,
	Double lat,
	Double lng,
	OffsetDateTime takenAt,
	OffsetDateTime updatedAt
) {
}
