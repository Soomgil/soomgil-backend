package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 태그별 최종 사용자 반응 집계 row.
 */
public record TagReactionAggregateRow(
	String tagId,
	Long positiveCount,
	Long reactionCount
) {
}
