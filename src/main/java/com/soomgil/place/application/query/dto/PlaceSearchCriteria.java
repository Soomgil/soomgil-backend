package com.soomgil.place.application.query.dto;

/**
 * 관광 원천 저장소에 전달하는 장소 검색 조건.
 *
 * @param q 검색어
 * @param bbox 지도 viewport 문자열
 * @param bboxMinLng bbox 최소 경도
 * @param bboxMinLat bbox 최소 위도
 * @param bboxMaxLng bbox 최대 경도
 * @param bboxMaxLat bbox 최대 위도
 * @param legalRegionCode 지역 코드
 * @param category 관광지 분류
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record PlaceSearchCriteria(
	String q,
	String bbox,
	Double bboxMinLng,
	Double bboxMinLat,
	Double bboxMaxLng,
	Double bboxMaxLat,
	String legalRegionCode,
	String category,
	int page,
	int size
) {

	/**
	 * 검색 API 입력값으로 검색 조건을 만든다.
	 *
	 * @param q 검색어
	 * @param bbox 지도 viewport 문자열
	 * @param legalRegionCode 지역 코드
	 * @param category 관광지 분류
	 * @param page 0 기반 page 번호
	 * @param size page 크기
	 */
	public PlaceSearchCriteria(String q, String bbox, String legalRegionCode, String category, int page, int size) {
		this(
			q,
			bbox,
			bboxPart(bbox, 0),
			bboxPart(bbox, 1),
			bboxPart(bbox, 2),
			bboxPart(bbox, 3),
			legalRegionCode,
			category,
			page,
			size
		);
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
