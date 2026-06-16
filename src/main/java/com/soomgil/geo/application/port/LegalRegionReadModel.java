package com.soomgil.geo.application.port;

import com.soomgil.geo.domain.model.LegalRegionLevel;

/**
 * 법정동 지역 read model.
 */
public record LegalRegionReadModel(
	String code,
	String name,
	String fullName,
	LegalRegionLevel level,
	String parentCode,
	boolean active
) {
}
