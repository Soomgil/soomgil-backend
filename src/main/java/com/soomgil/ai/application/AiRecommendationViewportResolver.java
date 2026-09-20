package com.soomgil.ai.application;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.api.dto.RegionViewportResponse;
import com.soomgil.place.application.query.handler.RegionViewportQueryHandler;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.trip.application.port.TripQueryRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * AI 추천 도구가 쓸 지도 범위(bbox)와 추천 탭을 결정한다.
 *
 * <p>LLM은 요청 컨텍스트의 지도 viewport를 직접 볼 수 없어 bbox를 비우거나 엉뚱한 값을 넘기기 쉽다.
 * 그래서 도구는 다음 순서로 범위를 스스로 확보한다:
 * 모델이 넘긴 bbox → 요청의 현재 지도 viewport → 여행방에 등록된 지역의 viewport.
 * 셋 다 없을 때만 명확한 메시지로 실패한다("viewport is required" 같은 즉시 예외로 대화를 끊지 않는다).
 */
@Component
public class AiRecommendationViewportResolver {

	private final TripQueryRepository tripRepository;
	private final RegionViewportQueryHandler regionViewportHandler;

	public AiRecommendationViewportResolver(
		TripQueryRepository tripRepository,
		RegionViewportQueryHandler regionViewportHandler
	) {
		this.tripRepository = Objects.requireNonNull(tripRepository, "tripRepository must not be null");
		this.regionViewportHandler = Objects.requireNonNull(regionViewportHandler, "regionViewportHandler must not be null");
	}

	/** {@code minLng,minLat,maxLng,maxLat} 형식의 bbox를 돌려준다. */
	public String resolveBbox(AiGuideRequest request, String suppliedBbox) {
		if (suppliedBbox != null && !suppliedBbox.isBlank()) return suppliedBbox.strip();

		var viewport = request.viewport();
		if (viewport != null && viewport.minLng() != null && viewport.minLat() != null
			&& viewport.maxLng() != null && viewport.maxLat() != null) {
			return viewport.minLng() + "," + viewport.minLat() + "," + viewport.maxLng() + "," + viewport.maxLat();
		}

		for (String regionCode : tripRepository.findTripRegionCodes(request.tripId())) {
			Optional<RegionViewportResponse> region = regionViewportHandler.handle(regionCode);
			if (region.isPresent()) {
				RegionViewportResponse r = region.get();
				return r.minLng() + "," + r.minLat() + "," + r.maxLng() + "," + r.maxLat();
			}
		}

		throw new BusinessException(
			ErrorCode.VALIDATION_FAILED,
			"추천할 지도 범위를 찾지 못했어요. 지도를 움직이거나 여행방에 지역을 설정해 주세요."
		);
	}

	/**
	 * 모델이 넘긴 중심 좌표가 쓸 만한지. null이거나 (0,0)처럼 의미 없는 값이면 bbox 중심으로 대체한다.
	 */
	public static boolean hasUsableCenter(Double lat, Double lng) {
		return lat != null && lng != null && !(lat == 0.0 && lng == 0.0);
	}

	/** bbox 중심 위도. 파싱에 실패하면 null. */
	public Double centerLat(String bbox) {
		double[] b = parse(bbox);
		return b == null ? null : (b[1] + b[3]) / 2;
	}

	/** bbox 중심 경도. 파싱에 실패하면 null. */
	public Double centerLng(String bbox) {
		double[] b = parse(bbox);
		return b == null ? null : (b[0] + b[2]) / 2;
	}

	/**
	 * 모델이 넘기는 탭 문자열을 관대하게 해석한다. 알 수 없는 값은 BASIC으로 본다.
	 * ("super like", "SUPERLIKE", "super_like" 등은 SUPER_LIKE)
	 */
	public static RecommendationTab parseTab(String tab) {
		if (tab == null || tab.isBlank()) return RecommendationTab.BASIC;
		String normalized = tab.strip().toUpperCase(Locale.ROOT).replaceAll("[\\s\\-]+", "_");
		if (normalized.equals("SUPER_LIKE") || normalized.equals("SUPERLIKE") || normalized.equals("SUPER")) {
			return RecommendationTab.SUPER_LIKE;
		}
		return RecommendationTab.BASIC;
	}

	private static double[] parse(String bbox) {
		if (bbox == null) return null;
		String[] parts = bbox.split(",");
		if (parts.length != 4) return null;
		try {
			return new double[] {
				Double.parseDouble(parts[0].strip()), Double.parseDouble(parts[1].strip()),
				Double.parseDouble(parts[2].strip()), Double.parseDouble(parts[3].strip()),
			};
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}
}
