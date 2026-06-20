package com.soomgil.geo.infrastructure.persistence.row;

/**
 * geo.legal_regions 테이블 row.
 */
public record LegalRegionRow(
	String code,
	String name,
	String fullName,
	String level,
	String parentCode,
	Boolean isActive
) {
}
