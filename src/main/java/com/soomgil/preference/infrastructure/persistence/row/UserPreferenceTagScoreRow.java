package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * 사용자 태그별 선호도 점수와 태그 메타데이터를 담은 조회 row.
 *
 * <p>그룹 집계와 개별 태그 노출 모두에 사용된다.
 *
 * @param tagId            태그 id
 * @param tagCode          태그 코드
 * @param displayName      태그 한국어 라벨
 * @param groupCode        그룹 코드
 * @param groupDisplayName 그룹 한국어 라벨
 * @param preferenceScore  0~1 범위 선호도 점수
 * @param likeCount        LIKE 반응 수
 * @param superLikeCount   SUPER_LIKE 반응 수
 * @param nopeCount        NOPE 반응 수
 */
public record UserPreferenceTagScoreRow(
	String tagId,
	String tagCode,
	String displayName,
	String groupCode,
	String groupDisplayName,
	BigDecimal preferenceScore,
	Integer likeCount,
	Integer superLikeCount,
	Integer nopeCount
) {
}
