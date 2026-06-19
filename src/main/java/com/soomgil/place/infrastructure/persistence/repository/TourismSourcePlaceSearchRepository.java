package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.application.query.dto.PlaceSearchItem;
import com.soomgil.place.application.query.dto.PlaceSearchResult;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourcePlaceSearchMapper;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceSearchRow;
import java.util.List;

/**
 * 관광 원천 저장소에서 장소 검색 결과를 조회하는 repository.
 */
public class TourismSourcePlaceSearchRepository {

	private final TourismSourcePlaceSearchMapper mapper;

	public TourismSourcePlaceSearchRepository(TourismSourcePlaceSearchMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 검색 조건에 맞는 관광 원천 장소 목록을 조회한다.
	 *
	 * @param criteria 검색 조건
	 * @return 검색 결과와 page metadata
	 */
	public PlaceSearchResult search(PlaceSearchCriteria criteria) {
		long totalElements = mapper.count(criteria);
		List<PlaceSearchItem> items = mapper.search(criteria)
			.stream()
			.map(this::toItem)
			.toList();

		return new PlaceSearchResult(items, new PageMeta(
			criteria.page(),
			criteria.size(),
			totalElements,
			totalPages(totalElements, criteria.size()),
			List.of()
		));
	}

	private PlaceSearchItem toItem(TourismSourcePlaceSearchRow row) {
		return new PlaceSearchItem(
			String.valueOf(row.contentId()),
			row.title(),
			row.address(),
			row.latitude(),
			row.longitude(),
			row.thumbnailUrl(),
			row.category(),
			PlaceSourceStatus.AVAILABLE
		);
	}

	private int totalPages(long totalElements, int size) {
		if (totalElements == 0) {
			return 0;
		}
		return (int) Math.ceil((double) totalElements / size);
	}
}
