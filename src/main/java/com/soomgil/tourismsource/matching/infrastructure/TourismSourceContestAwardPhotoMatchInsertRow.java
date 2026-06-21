package com.soomgil.tourismsource.matching.infrastructure;

import java.math.BigDecimal;

/**
 * 공모전 수상작 사진 매칭 insert row.
 *
 * @param id match id
 * @param photoId 수상작 사진 id
 * @param attractionNo 관광지 PK
 * @param sidoCode 시도 코드
 * @param gugunCode 구군 코드
 * @param matchScope 매칭 scope
 * @param matchStatus 매칭 status
 * @param matchMethod 매칭 method
 * @param confidence 신뢰도
 * @param selected 확정 여부
 * @param rationale 매칭 근거
 */
public record TourismSourceContestAwardPhotoMatchInsertRow(
	String id,
	String photoId,
	Integer attractionNo,
	Integer sidoCode,
	Integer gugunCode,
	String matchScope,
	String matchStatus,
	String matchMethod,
	BigDecimal confidence,
	boolean selected,
	String rationale
) {
}
