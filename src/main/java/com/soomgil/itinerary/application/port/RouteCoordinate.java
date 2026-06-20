package com.soomgil.itinerary.application.port;

/**
 * route matching 요청에 사용하는 경도/위도 좌표.
 *
 * @param lng 경도
 * @param lat 위도
 */
public record RouteCoordinate(
	double lng,
	double lat
) {
}
