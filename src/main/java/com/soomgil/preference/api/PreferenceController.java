package com.soomgil.preference.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReactionRequest;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}")
public class PreferenceController extends ApiControllerSupport {

	@GetMapping("/swipe-feed")
	public SwipeFeedResponse getSwipeFeed(
		@PathVariable UUID tripId,
		@RequestParam(required = false) RecommendationTab tab,
		@RequestParam(required = false) String seed,
		@RequestParam(defaultValue = "20") int limit
	) {
		return notImplemented();
	}

	@PostMapping("/places/{provider}/{externalPlaceId}/reactions")
	public SwipeReactionResponse reactToPlace(
		@PathVariable UUID tripId,
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId,
		@Valid @RequestBody SwipeReactionRequest request
	) {
		return notImplemented();
	}

	@GetMapping("/preference-summary")
	public Map<String, Object> getPreferenceSummary(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@GetMapping("/place-recommendations")
	public PagedPlaceRecommendation listPlaceRecommendations(
		@PathVariable UUID tripId,
		@RequestParam(required = false) RecommendationTab tab,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@GetMapping("/saved-places")
	public PagedSavedPlace listSavedPlaces(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@DeleteMapping("/saved-places/{savedPlaceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSavedPlace(@PathVariable UUID tripId, @PathVariable UUID savedPlaceId) {
		notImplemented();
	}
}
