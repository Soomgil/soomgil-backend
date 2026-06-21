package com.soomgil.place.application.query.dto;

import com.soomgil.common.api.dto.PageMeta;
import java.util.List;

/**
 * 관광 원천 검색 결과와 page metadata.
 *
 * @param items 검색 결과 목록
 * @param page page metadata
 */
public record PlaceSearchResult(
	List<PlaceSearchItem> items,
	PageMeta page
) {
}
