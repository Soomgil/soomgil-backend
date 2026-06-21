package com.soomgil.place.application.query.dto;

/**
 * 관광 원천 저장소에 전달하는 viewport 후보 조회 조건.
 *
 * @param bbox 지도 viewport 문자열
 * @param bboxMinLng bbox 최소 경도
 * @param bboxMinLat bbox 최소 위도
 * @param bboxMaxLng bbox 최대 경도
 * @param bboxMaxLat bbox 최대 위도
 * @param category 관광지 분류
 * @param limit 최대 후보 수
 */
public record PlaceViewportCandidateCriteria(
	String bbox,
	Double bboxMinLng,
	Double bboxMinLat,
	Double bboxMaxLng,
	Double bboxMaxLat,
	String category,
	int limit
) {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 200;

	/**
	 * query 입력값으로 viewport 후보 조회 조건을 만든다.
	 *
	 * @param bbox 지도 viewport 문자열
	 * @param category 관광지 분류
	 * @param limit 최대 후보 수
	 */
	public PlaceViewportCandidateCriteria(String bbox, String category, int limit) {
		this(
			bbox,
			bboxPart(bbox, 0),
			bboxPart(bbox, 1),
			bboxPart(bbox, 2),
			bboxPart(bbox, 3),
			category,
			normalizeLimit(limit)
		);
	}

	/**
	 * 조회 가능한 bbox인지 확인한다.
	 *
	 * @return bbox 네 좌표가 모두 있으면 {@code true}
	 */
	public boolean hasValidBounds() {
		return bboxMinLng != null
			&& bboxMinLat != null
			&& bboxMaxLng != null
			&& bboxMaxLat != null;
	}

	private static int normalizeLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}

	private static Double bboxPart(String bbox, int index) {
		if (bbox == null || bbox.isBlank()) {
			return null;
		}

		String[] parts = bbox.split(",");
		if (parts.length != 4) {
			return null;
		}

		try {
			return Double.valueOf(parts[index].trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
