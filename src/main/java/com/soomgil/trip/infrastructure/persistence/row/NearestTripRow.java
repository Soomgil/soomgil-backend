package com.soomgil.trip.infrastructure.persistence.row;

import java.time.LocalDate;

public record NearestTripRow(
	String id,
	String title,
	String displayDestination,
	LocalDate startDate,
	int memberCount,
	String coverImageUrl
) {
}
