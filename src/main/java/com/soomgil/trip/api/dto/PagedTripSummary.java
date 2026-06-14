package com.soomgil.trip.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedTripSummary(
	@Valid
	List<TripSummary> items,
	@Valid
	PageMeta page
) {
}
