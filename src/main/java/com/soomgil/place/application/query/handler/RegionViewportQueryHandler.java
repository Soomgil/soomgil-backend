package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.RegionViewportResponse;
import com.soomgil.place.application.port.KtoRegionCode;
import com.soomgil.place.application.port.RegionViewport;
import com.soomgil.place.application.port.TourismSourceRegionRepository;
import com.soomgil.place.application.service.LegalRegionKtoCodeResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 법정동 코드가 가리키는 지역의 지도 뷰포트를 관광 원천 장소 좌표로 계산한다.
 *
 * <p>법정동 코드를 KTO 시도/시군구로 바꾼 뒤 그 지역 장소들의 좌표 범위를 구한다. 시군구에 장소가 없으면
 * 시도 전체로 넓혀 다시 구한다. 어느 쪽에도 좌표가 없으면 empty를 돌려준다.
 */
@Service
public class RegionViewportQueryHandler {
	private final LegalRegionKtoCodeResolver resolver;
	private final TourismSourceRegionRepository regionRepository;

	public RegionViewportQueryHandler(
		LegalRegionKtoCodeResolver resolver,
		TourismSourceRegionRepository regionRepository
	) {
		this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
		this.regionRepository = Objects.requireNonNull(regionRepository, "regionRepository must not be null");
	}

	public Optional<RegionViewportResponse> handle(String legalRegionCode) {
		if (legalRegionCode == null || legalRegionCode.isBlank()) {
			return Optional.empty();
		}
		List<KtoRegionCode> codes = resolver.resolve(List.of(legalRegionCode.strip()));
		if (codes.isEmpty()) {
			return Optional.empty();
		}
		KtoRegionCode region = codes.get(0);
		int areaCode = Integer.parseInt(region.areaCode());
		Integer gugunCode = region.sigunguCode() == null ? null : Integer.valueOf(region.sigunguCode());

		Optional<RegionViewport> viewport = regionRepository.findRegionViewport(areaCode, gugunCode);
		if (viewport.isEmpty() && gugunCode != null) {
			// 시군구에 좌표 있는 장소가 없으면 시도 전체로 넓힌다.
			viewport = regionRepository.findRegionViewport(areaCode, null);
		}
		return viewport.map(RegionViewportQueryHandler::clampToRegion);
	}

	// 잘못 태깅된 소수 좌표가 경계를 지역 밖까지 벌리지 않도록, 경계를 중심 ±약 60km로 제한한다.
	private static final double MAX_DEGREE_DELTA = 0.6;

	private static RegionViewportResponse clampToRegion(RegionViewport v) {
		double minLat = Math.max(v.minLat(), v.centerLat() - MAX_DEGREE_DELTA);
		double maxLat = Math.min(v.maxLat(), v.centerLat() + MAX_DEGREE_DELTA);
		double minLng = Math.max(v.minLng(), v.centerLng() - MAX_DEGREE_DELTA);
		double maxLng = Math.min(v.maxLng(), v.centerLng() + MAX_DEGREE_DELTA);
		return new RegionViewportResponse(
			v.centerLat(), v.centerLng(), minLat, minLng, maxLat, maxLng, v.placeCount()
		);
	}
}
