package com.soomgil.preference.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.preference.api.dto.PagedSavedPlace;

/**
 * 현재 사용자의 저장 장소 목록을 조회하는 query.
 *
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record ListSavedPlacesQuery(
	int page,
	int size
) implements Query<PagedSavedPlace> {
}
