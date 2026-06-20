package com.soomgil.preference.application.command.dto;

import java.util.UUID;

/**
 * 태그 통계 재계산 결과.
 *
 * @param runId 생성한 통계 실행 ID
 * @param statisticCount 저장한 태그 통계 수
 */
public record RecalculateTagStatisticsResult(
	UUID runId,
	int statisticCount
) {
}
