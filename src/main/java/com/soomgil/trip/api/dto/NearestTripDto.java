package com.soomgil.trip.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NearestTripDto(
	UUID id,
	String title,
	String displayDestination,
	LocalDate startDate,
	int memberCount,
	List<String> memberThumbnails,
	String coverImageUrl
) {
}
