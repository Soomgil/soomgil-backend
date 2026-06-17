package com.soomgil.geo.api.dto;

import java.util.List;

public record SimplifiedCoordinates(
	List<LngLat> coordinates,
	Integer originalCount,
	Integer simplifiedCount,
	Integer maxPoints
) {
}
