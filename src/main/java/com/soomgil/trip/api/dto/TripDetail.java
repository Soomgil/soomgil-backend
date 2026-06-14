package com.soomgil.trip.api.dto;

import com.soomgil.geo.api.dto.LegalRegion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TripDetail(
	@NotNull
	UUID id,
	@NotBlank
	String title,
	String displayDestination,
	@NotNull
	TripStatus status,
	@NotNull
	TripAccessRole myRole,
	@NotNull
	Long itineraryVersion,
	@NotNull
	OffsetDateTime createdAt,
	UUID ownerUserId,
	@Valid
	List<LegalRegion> regions,
	@Valid
	List<TripMember> members,
	UUID retrippedFromPostId
) {
}
