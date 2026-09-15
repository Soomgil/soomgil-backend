package com.soomgil.itinerary.application.command.dto;

import java.net.URI;

/**
 * 일차 미정 그룹에 추가할 장소 한 건.
 *
 * <p>중복 판단 기준은 {@code placeProvider + externalPlaceId}이므로 두 값은 필수다.
 * 나머지 표시용 정보는 관광공사 원본을 다시 조회하지 않아도 화면에 그릴 수 있는 최소 정보다.
 *
 * @param placeProvider 장소 원천 provider. 예: {@code KTO}
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param placeName 장소명
 * @param address 주소. 없으면 null
 * @param lat 위도. 없으면 null
 * @param lng 경도. 없으면 null
 * @param thumbnailUrl 대표 이미지 URL. 없으면 null
 */
public record UnscheduledPlaceToAdd(
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl
) {
}
