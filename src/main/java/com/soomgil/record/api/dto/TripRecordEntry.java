package com.soomgil.record.api.dto;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TripRecordEntry(
	@NotNull
	UUID id,
	@NotNull
	UUID tripId,
	UUID itineraryDayId,
	UUID itineraryItemId,
	@Valid
	@NotNull
	UserSummary uploadedBy,
	String title,
	String caption,
	String locationName,
	Double lat,
	Double lng,
	OffsetDateTime takenAt,
	@NotNull
	RecordVisibility visibility,
	@NotBlank
	String status,
	@Valid
	List<MediaFile> media,
	@NotNull
	OffsetDateTime createdAt
) {
}
