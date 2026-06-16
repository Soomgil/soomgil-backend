package com.soomgil.geo.application.query.dto;

import com.soomgil.geo.domain.model.LegalRegionLevel;

/**
 * 법정동 지역 응답 view.
 */
public record LegalRegionView(
	String code,
	String name,
	String fullName,
	LegalRegionLevel level,
	String parentCode,
	boolean active
) {
}
