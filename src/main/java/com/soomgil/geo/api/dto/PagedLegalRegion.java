package com.soomgil.geo.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedLegalRegion(
	@Valid
	List<LegalRegion> items,
	@Valid
	PageMeta page
) {
}
