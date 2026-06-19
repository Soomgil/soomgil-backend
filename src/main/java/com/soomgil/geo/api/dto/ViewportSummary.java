package com.soomgil.geo.api.dto;

/**
 * 지도 viewport 검증/계산 응답.
 *
 * @param viewport 정규화된 viewport
 * @param center viewport 중심 좌표
 * @param widthMeters 대략적인 동서 폭(m)
 * @param heightMeters 대략적인 남북 높이(m)
 */
public record ViewportSummary(
	Viewport viewport,
	LngLat center,
	Double widthMeters,
	Double heightMeters
) {
}
