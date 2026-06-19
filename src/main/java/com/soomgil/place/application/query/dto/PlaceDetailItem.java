package com.soomgil.place.application.query.dto;

import com.soomgil.place.api.dto.PlaceSourceStatus;
import java.net.URI;
import java.time.OffsetDateTime;

/**
 * 관광 원천 상세 조회 결과.
 *
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param name 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 대표 이미지 URL
 * @param category 관광지 분류
 * @param sourceStatus 원천 데이터 상태
 * @param description 장소 설명
 * @param phone 전화번호
 * @param sourceUpdatedAt 원천 수정 시각
 * @param enriched 태그 enrichment 성공 여부
 */
public record PlaceDetailItem(
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String category,
	PlaceSourceStatus sourceStatus,
	String description,
	String phone,
	OffsetDateTime sourceUpdatedAt,
	Boolean enriched
) {
}
