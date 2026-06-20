package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsCommand;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsResult;
import com.soomgil.preference.domain.policy.PreferenceDiscriminationCalculator;
import com.soomgil.preference.domain.policy.RealUserServingTransitionPolicy;
import com.soomgil.preference.domain.policy.TagPreferenceStatistics;
import com.soomgil.preference.domain.policy.TagStatisticSource;
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
 *
 * <p>{@code REAL_USER}와 {@code SYNTHETIC_PERSONA}는 별도 조회 경로로 집계하며 한 run 안에서 섞지 않는다.
 */
@Service
public class PreferenceRecalculateTagStatisticsCommandHandler implements RecalculateTagStatisticsCommandHandler {

	private static final int RATE_SCALE = 12;
	private static final long MINIMUM_SYNTHETIC_REACTIONS_PER_CORE_TAG = 50;

	private final PreferenceTagStatisticsMapper mapper;
	private final PreferenceDiscriminationCalculator calculator;
	private final RealUserServingTransitionPolicy realUserTransitionPolicy;

	public PreferenceRecalculateTagStatisticsCommandHandler(PreferenceTagStatisticsMapper mapper) {
		this.mapper = mapper;
		this.calculator = new PreferenceDiscriminationCalculator();
		this.realUserTransitionPolicy = new RealUserServingTransitionPolicy(10_000, 100);
	}

	@Transactional
	@Override
	public RecalculateTagStatisticsResult handle(RecalculateTagStatisticsCommand command) {
		validate(command);
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			validateSyntheticQuality(command.generatorVersion());
		} else {
			validateRealUserTransition(command);
		}

		long totalReactionCount = totalReactionCount(command);
		if (totalReactionCount == 0) {
			throw new IllegalStateException("Cannot calculate tag statistics without source reactions.");
		}

		long positiveReactionCount = positiveReactionCount(command);
		BigDecimal globalPositiveRate = BigDecimal.valueOf(positiveReactionCount)
			.divide(BigDecimal.valueOf(totalReactionCount), RATE_SCALE, RoundingMode.HALF_UP);
		UUID runId = Ids.newUuid();
		mapper.insertRun(new TagStatisticRunInsertRow(
			runId.toString(),
			command.source().name(),
			command.priorReactionCount(),
			globalPositiveRate.setScale(6, RoundingMode.HALF_UP),
			totalReactionCount,
			positiveReactionCount
		));

		List<TagReactionAggregateRow> aggregates = aggregates(command);
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

	private long totalReactionCount(RecalculateTagStatisticsCommand command) {
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			return mapper.countSyntheticReactions(command.generatorVersion());
		}
		return mapper.countFinalReactions();
	}

	private long positiveReactionCount(RecalculateTagStatisticsCommand command) {
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			return mapper.countPositiveSyntheticReactions(command.generatorVersion());
		}
		return mapper.countPositiveFinalReactions();
	}

	private List<TagReactionAggregateRow> aggregates(RecalculateTagStatisticsCommand command) {
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			return mapper.aggregateSyntheticReactionsByTag(command.generatorVersion());
		}
		return mapper.aggregateFinalReactionsByTag();
	}

	private void validateSyntheticQuality(String generatorVersion) {
		long personaCount = mapper.countActiveSyntheticPersonas(generatorVersion);
		if (personaCount != 50) {
			throw new IllegalStateException("Synthetic statistics require exactly 50 active personas.");
		}
		if (mapper.countActiveSyntheticPersonasWithoutEvents(generatorVersion) > 0) {
			throw new IllegalStateException("Every active synthetic persona must have at least one event.");
		}
		if (mapper.countSyntheticCoreTagsBelowReactionMinimum(
			generatorVersion,
			MINIMUM_SYNTHETIC_REACTIONS_PER_CORE_TAG
		) > 0) {
			throw new IllegalStateException(
				"Every active preference tag must have at least 50 synthetic reactions."
			);
		}
	}

	private void validateRealUserTransition(RecalculateTagStatisticsCommand command) {
		if (!realUserTransitionPolicy.canPromote(
			mapper.countFinalReactions(),
			mapper.findRealCoreTagReactionCounts(),
			command.offlineEvaluationApproved()
		)) {
			throw new IllegalStateException("Real user statistics have not met the serving transition policy.");
		}
	}

	private void validate(RecalculateTagStatisticsCommand command) {
		if (command == null || command.priorReactionCount() == null
			|| command.priorReactionCount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("prior reaction count must be positive");
		}
		if (command.source() == null || command.source() == TagStatisticSource.AI_ONLY_DEFAULT) {
			throw new IllegalArgumentException("statistics source must be REAL_USER or SYNTHETIC_PERSONA");
		}
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA
			&& (command.generatorVersion() == null || command.generatorVersion().isBlank())) {
			throw new IllegalArgumentException("generator version is required for synthetic statistics");
		}
	}
}
