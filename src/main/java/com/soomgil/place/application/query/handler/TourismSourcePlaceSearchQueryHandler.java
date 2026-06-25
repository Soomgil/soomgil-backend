package com.soomgil.place.application.query.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceLiveSearchRequest;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 검색 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceSearchQueryHandler implements PlaceSearchQueryHandler {

	private final TourismPlaceFeedClient liveClient;

	public TourismSourcePlaceSearchQueryHandler(TourismPlaceFeedClient liveClient) {
		this.liveClient = liveClient;
	}

	@Override
	public PagedPlaceSummary handle(PlaceSearchQuery query) {
		List<PlaceSummary> liveItems = liveClient.fetchLive(new TourismPlaceLiveSearchRequest(
			query.q(),
			query.bbox(),
			query.legalRegionCode(),
			query.category(),
			query.size()
		)).stream()
			.map(this::toSummary)
			.toList();
		if (!liveItems.isEmpty()) {
			return new PagedPlaceSummary(
				liveItems,
				new PageMeta(query.page(), query.size(), (long) liveItems.size(), liveItems.isEmpty() ? 0 : 1, List.of())
			);
		}

		return new PagedPlaceSummary(
			liveItems,
			new PageMeta(query.page(), query.size(), (long) liveItems.size(), liveItems.isEmpty() ? 0 : 1, List.of())
		);
	}

	private PlaceSummary toSummary(TourismPlaceFeedItem item) {
		return new PlaceSummary(
			PlaceProvider.KTO,
			item.externalPlaceId(),
			item.name(),
			item.address(),
			item.lat(),
			item.lng(),
			toUri(item.thumbnailUrl()),
			item.category(),
			PlaceSourceStatus.AVAILABLE
		);
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}
}
