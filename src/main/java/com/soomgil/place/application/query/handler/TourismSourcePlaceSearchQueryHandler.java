package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.query.dto.PlaceSearchCriteria;
import com.soomgil.place.application.query.dto.PlaceSearchItem;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.dto.PlaceSearchResult;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceSearchRepository;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 검색 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceSearchQueryHandler implements PlaceSearchQueryHandler {

	private final TourismSourcePlaceSearchRepository repository;

	public TourismSourcePlaceSearchQueryHandler(TourismSourcePlaceSearchRepository repository) {
		this.repository = repository;
	}

	@Override
	public PagedPlaceSummary handle(PlaceSearchQuery query) {
		PlaceSearchResult result = repository.search(new PlaceSearchCriteria(
			query.q(),
			query.bbox(),
			query.legalRegionCode(),
			query.category(),
			query.page(),
			query.size()
		));

		return new PagedPlaceSummary(
			result.items().stream()
				.map(this::toSummary)
				.toList(),
			result.page()
		);
	}

	private PlaceSummary toSummary(PlaceSearchItem item) {
		return new PlaceSummary(
			PlaceProvider.KTO,
			item.externalPlaceId(),
			item.name(),
			item.address(),
			item.lat(),
			item.lng(),
			item.thumbnailUrl(),
			item.category(),
			item.sourceStatus()
		);
	}
}
