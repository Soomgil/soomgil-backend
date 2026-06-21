package com.soomgil.place.application.query.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import java.net.URI;

/**
 * 지도 viewport 안에서 추천/AI 후보로 사용할 장소.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param name 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 대표 이미지 URL
 * @param category 관광지 분류
 * @param sourceStatus 원천 데이터 상태
 */
public record PlaceViewportCandidate(
	PlaceProvider provider,
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String category,
	PlaceSourceStatus sourceStatus
) {
}
