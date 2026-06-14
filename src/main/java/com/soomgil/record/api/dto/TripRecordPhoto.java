package com.soomgil.record.api.dto;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripRecordPhoto(
	@NotNull
	UUID tripId,
	String tripTitle,
	@NotNull
	UUID recordId,
	UUID itineraryDayId,
	UUID itineraryItemId,
	@Valid
	@NotNull
	MediaFile media,
	@Valid
	UserSummary uploadedBy,
	OffsetDateTime takenAt,
	@NotNull
	OffsetDateTime createdAt
) {
}
