package com.soomgil.preference.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedPlaceRecommendation(
	@Valid
	List<PlaceRecommendation> items,
	@Valid
	PageMeta page
) {
}
