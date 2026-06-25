package com.soomgil.place.application.query.handler;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.application.service.KtoContentTypeResolver;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceDetailRepository;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 상세 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceDetailQueryHandler implements PlaceDetailQueryHandler {

	private final TourismSourcePlaceDetailRepository repository;
	private final TourismPlaceFeedClient liveClient;
	private final PlaceAccessibilityCacheService accessibilityCacheService;

	public TourismSourcePlaceDetailQueryHandler(
		TourismSourcePlaceDetailRepository repository,
		TourismPlaceFeedClient liveClient,
		PlaceAccessibilityCacheService accessibilityCacheService
	) {
		this.repository = repository;
		this.liveClient = liveClient;
		this.accessibilityCacheService = accessibilityCacheService;
	}

	@Override
	public PlaceDetail handle(PlaceDetailQuery query) {
		PlaceDetailItem item = findDetail(query);
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

	private PlaceDetailItem findDetail(PlaceDetailQuery query) {
		try {
			return repository.find(query);
		}
		catch (BusinessException exception) {
			if (exception.errorCode() != ErrorCode.RESOURCE_NOT_FOUND || query.provider() != PlaceProvider.KTO) {
				throw exception;
			}
			return liveClient.fetchOne(query.externalPlaceId())
				.map(this::toDetailItem)
				.orElseThrow(() -> exception);
		}
	}

	private PlaceDetailItem toDetailItem(TourismPlaceFeedItem item) {
		return new PlaceDetailItem(
			item.externalPlaceId(),
			item.name(),
			item.address(),
			item.lat(),
			item.lng(),
			toUri(item.thumbnailUrl()),
			item.photos().stream()
				.map(this::toUri)
				.filter(uri -> uri != null)
				.distinct()
				.toList(),
			item.category(),
			PlaceSourceStatus.AVAILABLE,
			item.description(),
			null,
			item.sourceModifiedAt(),
			false
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

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}
}
