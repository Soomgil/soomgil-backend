package com.soomgil.place.api.dto;

import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

public record PlaceDetail(
	@NotNull
	PlaceProvider provider,
	@NotBlank
	@Size(max = 120)
	String externalPlaceId,
	@NotBlank
	String name,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	List<URI> photos,
	String category,
	@NotNull
	PlaceSourceStatus sourceStatus,
	String description,
	String phone,
	OffsetDateTime sourceUpdatedAt,
	Boolean enriched,
	PlaceAccessibilityInfo accessibility
) {
	public PlaceDetail(
		PlaceProvider provider,
		String externalPlaceId,
		String name,
		String address,
		Double lat,
		Double lng,
		URI thumbnailUrl,
		List<URI> photos,
		String category,
		PlaceSourceStatus sourceStatus,
		String description,
		String phone,
		OffsetDateTime sourceUpdatedAt,
		Boolean enriched
	) {
		this(provider, externalPlaceId, name, address, lat, lng, thumbnailUrl, photos, category, sourceStatus,
			description, phone, sourceUpdatedAt, enriched, PlaceAccessibilityInfo.unknown());
	}

	public PlaceDetail {
		accessibility = accessibility == null ? PlaceAccessibilityInfo.unknown() : accessibility;
	}
}
