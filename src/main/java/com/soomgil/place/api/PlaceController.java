package com.soomgil.place.api;

import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.application.query.dto.PlaceSearchQuery;
import com.soomgil.place.application.query.dto.PopularPlacesQuery;
import com.soomgil.place.application.query.handler.PlaceDetailQueryHandler;
import com.soomgil.place.application.query.handler.PlaceSearchQueryHandler;
import com.soomgil.place.application.query.handler.PopularPlacesQueryHandler;
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

	public PlaceController(
		PlaceSearchQueryHandler placeSearchQueryHandler,
		PlaceDetailQueryHandler placeDetailQueryHandler,
		PopularPlacesQueryHandler popularPlacesQueryHandler
	) {
		this.placeSearchQueryHandler = placeSearchQueryHandler;
		this.placeDetailQueryHandler = placeDetailQueryHandler;
		this.popularPlacesQueryHandler = popularPlacesQueryHandler;
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

	@GetMapping("/{provider}/{externalPlaceId}")
	public PlaceDetail getPlace(
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId
	) {
		return placeDetailQueryHandler.handle(new PlaceDetailQuery(provider, externalPlaceId));
	}
}
