package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceDetailRepository;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 상세 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceDetailQueryHandler implements PlaceDetailQueryHandler {

	private final TourismSourcePlaceDetailRepository repository;

	public TourismSourcePlaceDetailQueryHandler(TourismSourcePlaceDetailRepository repository) {
		this.repository = repository;
	}

	@Override
	public PlaceDetail handle(PlaceDetailQuery query) {
		PlaceDetailItem item = repository.find(query);
		return new PlaceDetail(
			PlaceProvider.KTO,
			item.externalPlaceId(),
			item.name(),
			item.address(),
			item.lat(),
			item.lng(),
			item.thumbnailUrl(),
			item.photos(),
			item.category(),
			item.sourceStatus(),
			item.description(),
			item.phone(),
			item.sourceUpdatedAt(),
			item.enriched()
		);
	}
}
