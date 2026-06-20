package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 장소 태그, 멤버 선호도, 최종 SUPER_LIKE 여부를 함께 읽는 추천 계산 원천 row.
 *
 * <p>확정 태그가 없는 장소는 {@code tagId}, {@code confidence}, {@code weight}가 null일 수 있다.
 * 이 경우 handler는 중립 점수를 사용한다.
 */
public record RecommendationScoreSourceRow(
	String provider,
	String externalPlaceId,
	String tagId,
	BigDecimal confidence,
	BigDecimal weight,
	String userId,
	BigDecimal preferenceScore,
	String reaction,
	OffsetDateTime lastReactedAt,
	String displayName,
	String profileImageUrl
) {

	public RecommendationScoreSourceRow(
		String provider,
		String externalPlaceId,
		String tagId,
		BigDecimal confidence,
		BigDecimal weight,
		String userId,
		BigDecimal preferenceScore,
		String reaction
	) {
		this(
			provider,
			externalPlaceId,
			tagId,
			confidence,
			weight,
			userId,
			preferenceScore,
			reaction,
			null,
			null,
			null
		);
	}

	public RecommendationScoreSourceRow(
		String provider,
		String externalPlaceId,
		String tagId,
		BigDecimal confidence,
		BigDecimal weight,
		String userId,
		BigDecimal preferenceScore,
		String reaction,
		OffsetDateTime lastReactedAt
	) {
		this(
			provider,
			externalPlaceId,
			tagId,
			confidence,
			weight,
			userId,
			preferenceScore,
			reaction,
			lastReactedAt,
			null,
			null
		);
	}
}
