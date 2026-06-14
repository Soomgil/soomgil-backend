package com.soomgil.place.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController extends ApiControllerSupport {

	@GetMapping
	public PagedPlaceSummary searchPlaces(
		@RequestParam(required = false) String query,
		@RequestParam(required = false) String category,
		@RequestParam(required = false) String legalRegionCode,
		@RequestParam(required = false) PlaceProvider provider,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@GetMapping("/{provider}/{externalPlaceId}")
	public PlaceDetail getPlace(
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId
	) {
		return notImplemented();
	}
}
