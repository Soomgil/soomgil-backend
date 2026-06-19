package com.soomgil.place.application.query.dto;

import java.net.URI;

/**
 * 장소 화면에 노출할 이미지 후보.
 *
 * @param type 이미지 후보 원천 종류
 * @param publicUrl 공개 이미지 URL
 * @param sourceType 원천 이미지 세부 타입
 * @param displayOrder 노출 순서
 * @param width 이미지 너비
 * @param height 이미지 높이
 */
public record PlaceImageCandidate(
	PlaceImageCandidateType type,
	URI publicUrl,
	String sourceType,
	Integer displayOrder,
	Integer width,
	Integer height
) {
}
