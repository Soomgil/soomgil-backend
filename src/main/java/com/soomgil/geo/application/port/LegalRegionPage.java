package com.soomgil.geo.application.port;

import java.util.List;

/**
 * 법정동 지역 page read model.
 */
public record LegalRegionPage(
	List<LegalRegionReadModel> items,
	long totalElements
) {
}
