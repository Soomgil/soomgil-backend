package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.math.BigDecimal;

/**
 * 사용자별 최종 장소 반응으로 태그 좋아요 통계를 다시 계산하는 command.
 *
 * @param priorReactionCount 전체 좋아요율을 몇 개의 사전 반응으로 반영할지 나타내는 값
 */
public record RecalculateTagStatisticsCommand(
	BigDecimal priorReactionCount
) implements Command<RecalculateTagStatisticsResult> {
}
