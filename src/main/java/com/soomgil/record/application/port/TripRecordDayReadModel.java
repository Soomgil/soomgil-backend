package com.soomgil.record.application.port;

import java.time.LocalDate;
import java.util.UUID;

public record TripRecordDayReadModel(
	UUID id,
	Integer dayNumber,
	LocalDate date
) {
}
