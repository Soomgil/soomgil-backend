package com.soomgil.place.application.service;

import java.util.Map;

/**
 * KTO 카테고리 한글 이름 ↔ contentTypeId 매핑.
 * KtoTourismPlaceClient.CONTENT_TYPE_NAMES와 역관계.
 */
public final class KtoContentTypeResolver {

	private static final Map<String, String> NAME_TO_ID = Map.ofEntries(
		Map.entry("관광지", "12"),
		Map.entry("문화시설", "14"),
		Map.entry("축제·공연·행사", "15"),
		Map.entry("여행코스", "25"),
		Map.entry("레포츠", "28"),
		Map.entry("숙박", "32"),
		Map.entry("쇼핑", "38"),
		Map.entry("음식점", "39")
	);

	private KtoContentTypeResolver() {
	}

	public static String contentTypeIdFor(String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return null;
		}
		return NAME_TO_ID.get(categoryName.trim());
	}
}
