package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsCommand;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsResult;
import com.soomgil.preference.domain.policy.PreferenceDiscriminationCalculator;
import com.soomgil.preference.domain.policy.TagPreferenceStatistics;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceTagStatisticsMapper;
import com.soomgil.preference.infrastructure.persistence.row.TagReactionAggregateRow;
import com.soomgil.preference.infrastructure.persistence.row.TagStatisticInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.TagStatisticRunInsertRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최종 사용자 반응을 집계해 태그 통계를 만들고 성공한 실행 하나를 serving 상태로 승격한다.
 */
@Service
public class PreferenceRecalculateTagStatisticsCommandHandler implements RecalculateTagStatisticsCommandHandler {

	private static final int RATE_SCALE = 12;

	private final PreferenceTagStatisticsMapper mapper;
	private final PreferenceDiscriminationCalculator calculator;

	public PreferenceRecalculateTagStatisticsCommandHandler(PreferenceTagStatisticsMapper mapper) {
		this.mapper = mapper;
		this.calculator = new PreferenceDiscriminationCalculator();
	}

	@Transactional
	@Override
	public RecalculateTagStatisticsResult handle(RecalculateTagStatisticsCommand command) {
		long totalReactionCount = mapper.countFinalReactions();
		if (totalReactionCount == 0) {
			throw new IllegalStateException("Cannot calculate tag statistics without final reactions.");
		}

		long positiveReactionCount = mapper.countPositiveFinalReactions();
		BigDecimal globalPositiveRate = BigDecimal.valueOf(positiveReactionCount)
			.divide(BigDecimal.valueOf(totalReactionCount), RATE_SCALE, RoundingMode.HALF_UP);
		UUID runId = Ids.newUuid();
		mapper.insertRun(new TagStatisticRunInsertRow(
			runId.toString(),
			command.priorReactionCount(),
			globalPositiveRate.setScale(6, RoundingMode.HALF_UP),
			totalReactionCount,
			positiveReactionCount
		));

		List<TagReactionAggregateRow> aggregates = mapper.aggregateFinalReactionsByTag();
		for (TagReactionAggregateRow aggregate : aggregates) {
			TagPreferenceStatistics statistics = calculator.calculate(
				aggregate.positiveCount(),
				aggregate.reactionCount(),
				globalPositiveRate,
				command.priorReactionCount()
			);
			mapper.insertStatistic(new TagStatisticInsertRow(
				runId.toString(),
				aggregate.tagId(),
				statistics.preferenceDiscrimination(),
				statistics.smoothedPositiveRate(),
				aggregate.positiveCount(),
				aggregate.reactionCount()
			));
		}

		mapper.deactivateServingRuns();
		mapper.completeAndServeRun(runId.toString());
		return new RecalculateTagStatisticsResult(runId, aggregates.size());
	}
}
