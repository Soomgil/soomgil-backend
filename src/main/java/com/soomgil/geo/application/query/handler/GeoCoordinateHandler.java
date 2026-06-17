package com.soomgil.geo.application.query.handler;

import com.soomgil.geo.api.dto.LngLat;
import com.soomgil.geo.api.dto.Viewport;
import com.soomgil.geo.api.dto.ViewportSummary;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 지도 viewport와 좌표 목록을 검증하고 계산한다.
 */
@Component
public class GeoCoordinateHandler {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;
	private static final int DEFAULT_MAX_POINTS = 100;
	private static final int MIN_ROUTE_POINTS = 2;
	private static final int MAX_ROUTE_POINTS = 100;

	/**
	 * bbox 문자열을 정규화된 viewport와 중심/크기로 변환한다.
	 *
	 * @param bbox minLng,minLat,maxLng,maxLat
	 * @return viewport 요약
	 */
	public ViewportSummary summarizeViewport(String bbox) {
		Viewport viewport = parseBbox(bbox);
		LngLat center = new LngLat(
			(viewport.minLng() + viewport.maxLng()) / 2.0,
			(viewport.minLat() + viewport.maxLat()) / 2.0
		);
		double widthMeters = distanceMeters(center.lat(), viewport.minLng(), center.lat(), viewport.maxLng());
		double heightMeters = distanceMeters(viewport.minLat(), center.lng(), viewport.maxLat(), center.lng());
		return new ViewportSummary(viewport, center, widthMeters, heightMeters);
	}

	/**
	 * 좌표 목록을 maxPoints 이하로 단순화한다.
	 *
	 * <p>첫 좌표와 마지막 좌표는 항상 유지한다.
	 *
	 * @param coordinates 원본 좌표
	 * @param maxPoints 최대 좌표 수
	 * @return 단순화 좌표
	 */
	public List<LngLat> simplify(List<LngLat> coordinates, Integer maxPoints) {
		validateCoordinates(coordinates);
		int limit = maxPoints == null ? DEFAULT_MAX_POINTS : maxPoints;
		if (limit < MIN_ROUTE_POINTS || limit > MAX_ROUTE_POINTS) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Max points must be between 2 and 100.");
		}
		if (coordinates.size() <= limit) {
			return List.copyOf(coordinates);
		}

		List<LngLat> simplified = new ArrayList<>(limit);
		int lastIndex = coordinates.size() - 1;
		for (int index = 0; index < limit; index++) {
			int sourceIndex = (int) Math.round(index * (lastIndex / (double) (limit - 1)));
			simplified.add(coordinates.get(sourceIndex));
		}
		return simplified;
	}

	private Viewport parseBbox(String bbox) {
		if (bbox == null || bbox.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bbox is required.");
		}
		String[] parts = bbox.split(",");
		if (parts.length != 4) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bbox must be minLng,minLat,maxLng,maxLat.");
		}
		double minLng = parseCoordinate(parts[0], "minLng");
		double minLat = parseCoordinate(parts[1], "minLat");
		double maxLng = parseCoordinate(parts[2], "maxLng");
		double maxLat = parseCoordinate(parts[3], "maxLat");
		validateLng(minLng, "minLng");
		validateLng(maxLng, "maxLng");
		validateLat(minLat, "minLat");
		validateLat(maxLat, "maxLat");
		if (minLng >= maxLng || minLat >= maxLat) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bbox min values must be smaller than max values.");
		}
		return new Viewport(minLng, minLat, maxLng, maxLat);
	}

	private void validateCoordinates(List<LngLat> coordinates) {
		if (coordinates == null || coordinates.size() < MIN_ROUTE_POINTS) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Coordinates must contain at least 2 points.");
		}
		for (LngLat coordinate : coordinates) {
			if (coordinate == null) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Coordinates must not contain null.");
			}
			validateLng(coordinate.lng(), "lng");
			validateLat(coordinate.lat(), "lat");
		}
	}

	private double parseCoordinate(String value, String fieldName) {
		try {
			return Double.parseDouble(value.trim());
		}
		catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + " is invalid.");
		}
	}

	private void validateLng(Double value, String fieldName) {
		if (value == null || value < -180.0 || value > 180.0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + " must be between -180 and 180.");
		}
	}

	private void validateLat(Double value, String fieldName) {
		if (value == null || value < -90.0 || value > 90.0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + " must be between -90 and 90.");
		}
	}

	private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double deltaLat = Math.toRadians(lat2 - lat1);
		double deltaLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(deltaLng / 2.0) * Math.sin(deltaLng / 2.0);
		return EARTH_RADIUS_METERS * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
	}
}
