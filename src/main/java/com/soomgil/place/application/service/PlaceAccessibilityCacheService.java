package com.soomgil.place.application.service;

import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 장소 접근성 정보 조회의 퍼사드.
 * 단건 조회는 {@link PlaceAccessibilityCacheBackend#load}에 위임하여 {@code @Cacheable}이 적용된다.
 * 다건 조회는 단건 호출을 병렬로 수행하며, 캐시 hit/miss는 각 건별로 처리된다.
 */
@Service
public class PlaceAccessibilityCacheService {

	public record PlaceRef(String provider, String externalPlaceId, String contentTypeId) {
		public String cacheKey() {
			return provider + ":" + externalPlaceId;
		}
	}

	private final PlaceAccessibilityCacheBackend backend;

	public PlaceAccessibilityCacheService(PlaceAccessibilityCacheBackend backend) {
		this.backend = backend;
	}

	public PlaceAccessibilityInfo get(String provider, String externalPlaceId, String contentTypeId) {
		return backend.load(provider, externalPlaceId, contentTypeId);
	}

	public Map<String, PlaceAccessibilityInfo> getMany(List<PlaceRef> refs) {
		if (refs == null || refs.isEmpty()) {
			return Map.of();
		}
		return refs.parallelStream()
			.collect(Collectors.toMap(
				PlaceRef::cacheKey,
				this::loadOrUnknown,
				(left, right) -> left,
				LinkedHashMap::new
			));
	}

	private PlaceAccessibilityInfo loadOrUnknown(PlaceRef ref) {
		try {
			return backend.load(ref.provider(), ref.externalPlaceId(), ref.contentTypeId());
		}
		catch (RuntimeException exception) {
			return PlaceAccessibilityInfo.unknown();
		}
	}
}
