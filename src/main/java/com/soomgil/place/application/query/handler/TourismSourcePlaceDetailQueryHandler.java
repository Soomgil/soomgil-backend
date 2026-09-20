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
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 상세 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceDetailQueryHandler implements PlaceDetailQueryHandler {
	private static final String RETIRED_DEMO_CDN_HOST = "daobk0bynum21.cloudfront.net";

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
			query.includeInfo() ? accessibility(item) : PlaceAccessibilityInfo.unknown()
		);
	}

	private PlaceDetailItem findDetail(PlaceDetailQuery query) {
		try {
			PlaceDetailItem item = repository.find(query);
			boolean refresh = liveClient.refreshRequested(query.externalPlaceId());
			boolean incomplete = item.photos().size() < 2
				|| item.description() == null
				|| item.description().isBlank()
				|| !isUsableImage(item.thumbnailUrl())
				|| item.photos().stream().anyMatch(photo -> !isUsableImage(photo));
			if (refresh || incomplete) {
				return liveClient.fetchOne(query.externalPlaceId())
					.map(this::toDetailItem)
					.map(fresh -> merge(item, fresh, refresh))
					.orElse(item);
			}
			return item;
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

	private PlaceDetailItem merge(PlaceDetailItem stored, PlaceDetailItem fresh, boolean refresh) {
		List<URI> freshImages = Stream.concat(
			Stream.of(fresh.thumbnailUrl()),
			fresh.photos().stream()
		).filter(this::isUsableImage).toList();
		URI thumbnail = isUsableImage(stored.thumbnailUrl())
			? stored.thumbnailUrl()
			: freshImages.stream().findFirst().orElse(null);
		List<URI> photos = Stream.concat(
			stored.photos().stream().filter(this::isUsableImage),
			freshImages.stream()
		).distinct().toList();
		String description = refresh && fresh.description() != null
			? fresh.description()
			: stored.description() == null || stored.description().isBlank()
				? fresh.description()
				: stored.description();
		return new PlaceDetailItem(
			stored.externalPlaceId(),
			stored.name(),
			stored.address(),
			stored.lat(),
			stored.lng(),
			thumbnail,
			photos,
			stored.category(),
			stored.sourceStatus(),
			description,
			stored.phone(),
			stored.sourceUpdatedAt(),
			stored.enriched()
		);
	}

	private boolean isUsableImage(URI uri) {
		return uri != null && !RETIRED_DEMO_CDN_HOST.equalsIgnoreCase(uri.getHost());
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
