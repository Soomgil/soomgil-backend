package com.soomgil.geo.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import java.util.List;

/**
 * 법정동 지역 목록 조회 query.
 */
public record ListLegalRegionsQuery(
	String query,
	LegalRegionLevel level,
	String parentCode,
	Boolean isActive,
	int page,
	int size,
	List<String> sort
) implements Query<PagedLegalRegionView> {

	public ListLegalRegionsQuery {
		query = normalizeText(query);
		parentCode = normalizeText(parentCode);
		sort = sort == null ? List.of() : List.copyOf(sort);
	}

	private static String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
