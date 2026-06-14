package com.soomgil.place.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedPlaceSummary(
	@Valid
	List<PlaceSummary> items,
	@Valid
	PageMeta page
) {
}
