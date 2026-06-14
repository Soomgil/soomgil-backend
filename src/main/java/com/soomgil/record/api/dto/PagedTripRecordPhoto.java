package com.soomgil.record.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedTripRecordPhoto(
	@Valid
	List<TripRecordPhoto> items,
	@Valid
	PageMeta page
) {
}
