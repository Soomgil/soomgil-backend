package com.soomgil.preference.api;

import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.application.query.handler.ListPlaceRecommendationsQueryHandler;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}")
public class PreferenceController {

	private final ListPlaceRecommendationsQueryHandler recommendationHandler;

	public PreferenceController(ListPlaceRecommendationsQueryHandler recommendationHandler) {
		this.recommendationHandler = recommendationHandler;
	}

	@GetMapping("/place-recommendations")
	public PagedPlaceRecommendation listPlaceRecommendations(
		@PathVariable UUID tripId,
		@RequestParam String bbox,
		@RequestParam(required = false) Double centerLat,
		@RequestParam(required = false) Double centerLng,
		@RequestParam(required = false) RecommendationTab tab,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return recommendationHandler.handle(new ListPlaceRecommendationsQuery(
			tripId,
			bbox,
			centerLat,
			centerLng,
			tab,
			page,
			size
		));
	}
}
