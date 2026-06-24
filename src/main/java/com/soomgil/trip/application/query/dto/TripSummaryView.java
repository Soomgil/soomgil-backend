package com.soomgil.trip.application.query.dto;

import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 여행방 목록 item view.
 *
 * <p>{@code myRole}은 요청 사용자 기준으로 파생된 access role이다.
 */
public record TripSummaryView(
	UUID id,
	String title,
	String displayDestination,
	TripStatus status,
	TripAccessRole myRole,
	long itineraryVersion,
	LocalDate startDate,
	LocalDate endDate,
	Instant createdAt
) {
}
