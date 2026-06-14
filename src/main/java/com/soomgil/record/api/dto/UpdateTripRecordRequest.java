package com.soomgil.record.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UpdateTripRecordRequest(
	UUID itineraryDayId,
	UUID itineraryItemId,
	String title,
	String caption,
	String locationName,
	Double lat,
	Double lng,
	OffsetDateTime takenAt,
	List<UUID> mediaFileIds
) {
}
