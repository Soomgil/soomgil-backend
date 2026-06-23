package com.soomgil.place.api;

import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장소 접근성/운영시간/주차 일괄 조회 API.
 * 동일한 캐시를 스와이프 피드와 공유하므로, 한쪽에서 캐싱된 장소는 다른쪽에서 빠르게 조회된다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/places/accessibility")
public class PlaceAccessibilityController {

	private static final int MAX_BATCH = 100;

	private final PlaceAccessibilityCacheService accessibilityCacheService;

	public PlaceAccessibilityController(PlaceAccessibilityCacheService accessibilityCacheService) {
		this.accessibilityCacheService = accessibilityCacheService;
	}

	@PostMapping("/batch")
	public BatchResponse batch(@Valid @NotNull @RequestBody BatchRequest request) {
		List<PlaceAccessibilityCacheService.PlaceRef> refs = request.items().stream()
			.map(item -> new PlaceAccessibilityCacheService.PlaceRef(
				item.provider(),
				item.externalPlaceId(),
				item.contentTypeId()
			))
			.limit(MAX_BATCH)
			.toList();
		Map<String, PlaceAccessibilityInfo> map = accessibilityCacheService.getMany(refs);
		return new BatchResponse(map);
	}

	public record BatchRequest(
		@NotNull @Size(min = 1, max = MAX_BATCH) List<Item> items
	) {
		public record Item(
			@NotNull String provider,
			@NotNull String externalPlaceId,
			String contentTypeId
		) {
		}
	}

	public record BatchResponse(Map<String, PlaceAccessibilityInfo> map) {
	}
}
