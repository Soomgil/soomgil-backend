package com.soomgil.place.api;

import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.RegionViewportResponse;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.dto.PopularPlacesQuery;
import com.soomgil.place.application.query.handler.PlaceDetailQueryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.place.application.query.handler.PopularPlacesQueryHandler;
import com.soomgil.place.application.query.handler.RegionViewportQueryHandler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

	private final PlaceSearchQueryHandler placeSearchQueryHandler;
	private final PlaceDetailQueryHandler placeDetailQueryHandler;
	private final PopularPlacesQueryHandler popularPlacesQueryHandler;
	private final RegionViewportQueryHandler regionViewportQueryHandler;

	public PlaceController(
		PlaceSearchQueryHandler placeSearchQueryHandler,
		PlaceDetailQueryHandler placeDetailQueryHandler,
		PopularPlacesQueryHandler popularPlacesQueryHandler,
		RegionViewportQueryHandler regionViewportQueryHandler
	) {
		this.placeSearchQueryHandler = placeSearchQueryHandler;
		this.placeDetailQueryHandler = placeDetailQueryHandler;
		this.popularPlacesQueryHandler = popularPlacesQueryHandler;
		this.regionViewportQueryHandler = regionViewportQueryHandler;
	}

	@GetMapping("/search")
	public PagedPlaceSummary searchPlaces(
		@RequestParam(name = "q", required = false) String q,
		@RequestParam(required = false) String bbox,
		@RequestParam(required = false) String category,
		@RequestParam(required = false) String legalRegionCode,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return placeSearchQueryHandler.handle(new PlaceSearchQuery(q, bbox, legalRegionCode, category, page, size));
	}

	@GetMapping("/popular")
	public PagedPlaceSummary getPopularPlaces(
		@RequestParam(defaultValue = "3") int limit
	) {
		return popularPlacesQueryHandler.handle(new PopularPlacesQuery(limit));
	}

	/**
	 * 여행지역(법정동 코드)의 지도 뷰포트를 돌려준다. 담은 장소가 없는 여행방에서 지도를 그 지역으로
	 * 옮기기 위한 것으로, 해당 지역에 좌표가 있는 관광 원천 장소가 없으면 204(빈 응답)를 준다.
	 */
	@GetMapping("/region-viewport")
	public org.springframework.http.ResponseEntity<RegionViewportResponse> getRegionViewport(
		@RequestParam String legalRegionCode
	) {
		return regionViewportQueryHandler.handle(legalRegionCode)
			.map(org.springframework.http.ResponseEntity::ok)
			.orElseGet(() -> org.springframework.http.ResponseEntity.noContent().build());
	}

	@GetMapping("/{provider}/{externalPlaceId}")
	public PlaceDetail getPlace(
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId,
        @RequestParam(defaultValue = "true") boolean includeInfo
	) {
		return placeDetailQueryHandler.handle(new PlaceDetailQuery(provider, externalPlaceId, includeInfo));
	}
}
