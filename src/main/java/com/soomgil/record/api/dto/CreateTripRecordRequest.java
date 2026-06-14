package com.soomgil.record.api.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateTripRecordRequest(
	UUID itineraryDayId,
	UUID itineraryItemId,
	@Size(max = 160)
	String title,
	String caption,
	String locationName,
	Double lat,
	Double lng,
	OffsetDateTime takenAt,
	List<UUID> mediaFileIds
) {
}
