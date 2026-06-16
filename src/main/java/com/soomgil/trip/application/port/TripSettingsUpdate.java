package com.soomgil.trip.application.port;

import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * trip.trips 기본 설정 갱신에 필요한 값.
 *
 * <p>{@code displayDestinationProvided}가 false이면 {@code displayDestination}은 무시된다.
 */
public record TripSettingsUpdate(
	UUID tripId,
	String title,
	boolean displayDestinationProvided,
	String displayDestination,
	TripStatus status,
	Instant updatedAt
) {

	public TripSettingsUpdate {
		Objects.requireNonNull(tripId, "tripId must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
