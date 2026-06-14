package com.soomgil.record.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedTripRecordEntry(
	@Valid
	List<TripRecordEntry> items,
	@Valid
	PageMeta page
) {
}
