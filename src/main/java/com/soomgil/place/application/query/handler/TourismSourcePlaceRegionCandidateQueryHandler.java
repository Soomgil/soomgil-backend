package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.port.KtoRegionCode;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.place.application.port.TourismPlaceLiveSearchRequest;
import com.soomgil.place.application.query.dto.PlaceRegionCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.service.LegalRegionKtoCodeResolver;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 여행 지역 기반 장소 후보 query를 처리한다.
 *
 * <p>여행방이 저장한 법정동 코드는 관광 원천이 이해하는 코드가 아니므로, 먼저
 * {@link LegalRegionKtoCodeResolver}로 KTO area/sigungu 코드로 바꾼 뒤 지역별로 조회한다.
 * 여러 지역이 등록된 여행방을 지원하기 위해 external place id 기준으로 중복을 제거한다.
 * 변환되는 지역이 하나도 없으면 {@code keyword}로 대체 검색하고, 둘 다 없으면 빈 목록을 반환한다.
 */
@Service
public class TourismSourcePlaceRegionCandidateQueryHandler implements PlaceRegionCandidateQueryHandler {
	private static final int MAX_LIMIT = 200;

	private final TourismPlaceFeedClient liveClient;
	private final LegalRegionKtoCodeResolver regionCodeResolver;

	public TourismSourcePlaceRegionCandidateQueryHandler(
		TourismPlaceFeedClient liveClient,
		LegalRegionKtoCodeResolver regionCodeResolver
	) {
		this.liveClient = Objects.requireNonNull(liveClient, "liveClient must not be null");
		this.regionCodeResolver = Objects.requireNonNull(regionCodeResolver, "regionCodeResolver must not be null");
	}

	@Override
	public List<PlaceViewportCandidate> handle(PlaceRegionCandidateQuery query) {
		int limit = normalizeLimit(query.limit());
		Map<String, PlaceViewportCandidate> unique = new LinkedHashMap<>();
		List<String> legalRegionCodes = query.legalRegionCodes() == null ? List.of() : query.legalRegionCodes();
		for (KtoRegionCode region : regionCodeResolver.resolve(legalRegionCodes)) {
			collectInto(unique, new TourismPlaceLiveSearchRequest(
				null, null, region.areaCode(), query.category(), limit, region.sigunguCode()
			));
		}
		if (unique.isEmpty() && query.keyword() != null && !query.keyword().isBlank()) {
			collectInto(unique, new TourismPlaceLiveSearchRequest(
				query.keyword().strip(), null, null, query.category(), limit
			));
		}
		List<PlaceViewportCandidate> result = new ArrayList<>(unique.values());
		return result.size() <= limit ? List.copyOf(result) : List.copyOf(result.subList(0, limit));
	}

	private void collectInto(Map<String, PlaceViewportCandidate> unique, TourismPlaceLiveSearchRequest request) {
		for (TourismPlaceFeedItem item : liveClient.fetchLive(request)) {
			if (item.externalPlaceId() == null || item.externalPlaceId().isBlank()) {
				continue;
			}
			unique.putIfAbsent(item.externalPlaceId(), toCandidate(item));
		}
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

	private int normalizeLimit(int limit) {
		if (limit < 1) {
			return 20;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
