package com.soomgil.itinerary.application.port;

import java.util.List;

/**
 * 외부 map matching provider 요청 모델.
 *
 * @param providerProfile provider profile
 * @param coordinates 원본 좌표 목록
 * @param radiuses 좌표별 탐색 반경
 * @param tidy provider tidy 옵션
 */
public record MapMatchClientRequest(
	String providerProfile,
	List<RouteCoordinate> coordinates,
	List<Double> radiuses,
	Boolean tidy
) {
}
