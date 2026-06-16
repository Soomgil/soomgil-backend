package com.soomgil.geo.application.query.dto;

import java.util.List;

/**
 * 법정동 지역 page view.
 */
public record PagedLegalRegionView(
	List<LegalRegionView> items,
	int page,
	int size,
	long totalElements,
	int totalPages,
	List<String> sort
) {
}
