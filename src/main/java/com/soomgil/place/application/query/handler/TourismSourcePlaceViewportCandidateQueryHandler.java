package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceLiveSearchRequest;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateQuery;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 viewport 장소 후보 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceViewportCandidateQueryHandler implements PlaceViewportCandidateQueryHandler {

	private final TourismPlaceFeedClient liveClient;

	public TourismSourcePlaceViewportCandidateQueryHandler(TourismPlaceFeedClient liveClient) {
		this.liveClient = liveClient;
	}

	@Override
	public List<PlaceViewportCandidate> handle(PlaceViewportCandidateQuery query) {
		List<PlaceViewportCandidate> liveCandidates = liveClient.fetchLive(new TourismPlaceLiveSearchRequest(
			null,
			query.bbox(),
			null,
			query.category(),
			query.limit()
		)).stream()
			.map(this::toCandidate)
			.toList();
		if (!liveCandidates.isEmpty()) {
			return liveCandidates;
		}
		return List.of();
	}

	private PlaceViewportCandidate toCandidate(TourismPlaceFeedItem item) {
		return new PlaceViewportCandidate(
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
