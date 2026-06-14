package com.soomgil.geo.api.dto;

public record Viewport(
	Double minLng,
	Double minLat,
	Double maxLng,
	Double maxLat
) {
}
