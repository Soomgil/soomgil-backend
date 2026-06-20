package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.preference.domain.policy.TagStatisticSource;
import java.math.BigDecimal;

/**
 * 사용자별 최종 장소 반응으로 태그 좋아요 통계를 다시 계산하는 command.
 *
 * @param priorReactionCount 전체 좋아요율을 몇 개의 사전 반응으로 반영할지 나타내는 값
 * @param source 서로 섞지 않고 집계할 반응 데이터 source
 * @param generatorVersion 합성 source에서 집계할 generator version
 * @param offlineEvaluationApproved 실제 사용자 표본 기준을 대체할 offline 평가 승인 여부
 */
public record RecalculateTagStatisticsCommand(
	BigDecimal priorReactionCount,
	TagStatisticSource source,
	String generatorVersion,
	boolean offlineEvaluationApproved
) implements Command<RecalculateTagStatisticsResult> {

	public RecalculateTagStatisticsCommand(BigDecimal priorReactionCount) {
		this(priorReactionCount, TagStatisticSource.REAL_USER, null, false);
	}

	public RecalculateTagStatisticsCommand(
		BigDecimal priorReactionCount,
		TagStatisticSource source,
		String generatorVersion
	) {
		this(priorReactionCount, source, generatorVersion, false);
	}
}
