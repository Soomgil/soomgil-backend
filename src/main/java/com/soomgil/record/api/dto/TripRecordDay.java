package com.soomgil.record.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TripRecordDay(
	UUID id,
	Integer dayNumber,
	LocalDate date
) {
}
