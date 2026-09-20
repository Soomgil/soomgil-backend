package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import java.util.List;

/**
 * 가입 취향 설문에 노출하는 고정 관광지 카드.
 *
 * <p>장소 순서와 표현 정보는 활성 survey version에 종속되며, 클라이언트는 {@code sortOrder} 순서로
 * 모든 카드를 평가해야 한다.
 */
public record OnboardingPreferencePlace(
	PlaceProvider provider,
	String externalPlaceId,
	String name,
	String address,
	String thumbnailUrl,
	String category,
	String description,
	List<String> tags,
	int sortOrder
) {
}
