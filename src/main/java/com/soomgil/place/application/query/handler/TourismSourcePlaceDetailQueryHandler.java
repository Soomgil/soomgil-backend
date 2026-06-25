package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.application.service.KtoContentTypeResolver;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceDetailRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 상세 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceDetailQueryHandler implements PlaceDetailQueryHandler {

	private final TourismSourcePlaceDetailRepository repository;
	private final PlaceAccessibilityCacheService accessibilityCacheService;

	public TourismSourcePlaceDetailQueryHandler(
		TourismSourcePlaceDetailRepository repository,
		PlaceAccessibilityCacheService accessibilityCacheService
	) {
		this.repository = repository;
		this.accessibilityCacheService = accessibilityCacheService;
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
			item.enriched(),
			accessibility(item)
		);
	}

	private PlaceAccessibilityInfo accessibility(PlaceDetailItem item) {
		String provider = PlaceProvider.KTO.name();
		String externalPlaceId = item.externalPlaceId();
		Map<String, PlaceAccessibilityInfo> result = accessibilityCacheService.getMany(List.of(
			new PlaceAccessibilityCacheService.PlaceRef(
				provider,
				externalPlaceId,
				KtoContentTypeResolver.contentTypeIdFor(item.category())
			)
		));
		return result.getOrDefault(provider + ":" + externalPlaceId, PlaceAccessibilityInfo.unknown());
	}
}
