package com.soomgil.place.application.query.dto;

import com.soomgil.place.api.dto.PlaceSourceStatus;
import java.net.URI;

/**
 * 관광 원천 검색 결과 한 건.
 *
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param name 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 대표 이미지 URL
 * @param category 관광지 분류
 * @param sourceStatus 원천 데이터 상태
 */
public record PlaceSearchItem(
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
