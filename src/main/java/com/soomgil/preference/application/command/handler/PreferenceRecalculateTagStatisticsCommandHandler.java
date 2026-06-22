package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsCommand;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsResult;
import com.soomgil.preference.config.PreferencePolicyProperties;
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
	private final PreferenceTagStatisticsMapper mapper;
	private final PreferenceDiscriminationCalculator calculator;
	private final RealUserServingTransitionPolicy realUserTransitionPolicy;
	private final BigDecimal defaultPriorReactionCount;
	private final long minimumSyntheticReactionsPerCoreTag;

	public PreferenceRecalculateTagStatisticsCommandHandler(
		PreferenceTagStatisticsMapper mapper,
		PreferencePolicyProperties properties
	) {
		this.mapper = mapper;
		this.calculator = new PreferenceDiscriminationCalculator();
		this.defaultPriorReactionCount = properties.getStatistics().getAlpha();
		this.minimumSyntheticReactionsPerCoreTag = properties.getSyntheticPersona()
			.getMinimumCoreTagReactionCount();
		this.realUserTransitionPolicy = new RealUserServingTransitionPolicy(
			properties.getRealUser().getMinimumTotalReactionCount(),
			properties.getRealUser().getMinimumCoreTagReactionCount()
		);
	}

	@Transactional
	@Override
	public RecalculateTagStatisticsResult handle(RecalculateTagStatisticsCommand command) {
		validate(command);
		if (command.source() == TagStatisticSource.AI_ONLY_DEFAULT) {
			return initializeAiOnlyDefault(command);
		}
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			if (TagStatisticSource.REAL_USER.name().equals(mapper.findServingRunSource())) {
				throw new IllegalStateException(
					"Synthetic statistics cannot replace REAL_USER serving statistics."
				);
			}
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
		BigDecimal priorReactionCount = priorReactionCount(command);
		UUID runId = Ids.newUuid();
		mapper.insertRun(new TagStatisticRunInsertRow(
			runId.toString(),
			command.source().name(),
			priorReactionCount,
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
				priorReactionCount
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

	private RecalculateTagStatisticsResult initializeAiOnlyDefault(RecalculateTagStatisticsCommand command) {
		String servingSource = mapper.findServingRunSource();
		if (servingSource != null && !TagStatisticSource.AI_ONLY_DEFAULT.name().equals(servingSource)) {
			throw new IllegalStateException("AI only default cannot replace a higher serving statistics source.");
		}
		BigDecimal neutralRate = new BigDecimal("0.5");
		UUID runId = Ids.newUuid();
		mapper.insertRun(new TagStatisticRunInsertRow(
			runId.toString(),
			TagStatisticSource.AI_ONLY_DEFAULT.name(),
			priorReactionCount(command),
			neutralRate,
			0,
			0
		));
		List<String> tagIds = mapper.findActiveSelectableTagIds();
		for (String tagId : tagIds) {
			mapper.insertStatistic(new TagStatisticInsertRow(
				runId.toString(),
				tagId,
				neutralRate,
				neutralRate,
				0,
				0
			));
		}
		mapper.deactivateServingRuns();
		mapper.completeAndServeRun(runId.toString());
		return new RecalculateTagStatisticsResult(runId, tagIds.size());
	}

	private long totalReactionCount(RecalculateTagStatisticsCommand command) {
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			return mapper.countSyntheticReactions(command.generatorVersion());
		}
		return mapper.countRealSwipeEvents();
	}

	private long positiveReactionCount(RecalculateTagStatisticsCommand command) {
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			return mapper.countPositiveSyntheticReactions(command.generatorVersion());
		}
		return mapper.countPositiveRealSwipeEvents();
	}

	private List<TagReactionAggregateRow> aggregates(RecalculateTagStatisticsCommand command) {
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA) {
			return mapper.aggregateSyntheticReactionsByTag(command.generatorVersion());
		}
		return mapper.aggregateRealSwipeEventsByTag();
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
			minimumSyntheticReactionsPerCoreTag
		) > 0) {
			throw new IllegalStateException(
				"Every active preference tag must have at least "
					+ minimumSyntheticReactionsPerCoreTag
					+ " synthetic reactions."
			);
		}
	}

	private void validateRealUserTransition(RecalculateTagStatisticsCommand command) {
		if (!realUserTransitionPolicy.canPromote(
			mapper.countRealSwipeEvents(),
			mapper.findRealCoreTagReactionCounts(),
			command.offlineEvaluationApproved()
		)) {
			throw new IllegalStateException("Real user statistics have not met the serving transition policy.");
		}
	}

	private void validate(RecalculateTagStatisticsCommand command) {
		if (command == null || priorReactionCount(command).compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("prior reaction count must be positive");
		}
		if (command.source() == null) {
			throw new IllegalArgumentException("statistics source must not be null");
		}
		if (command.source() == TagStatisticSource.SYNTHETIC_PERSONA
			&& (command.generatorVersion() == null || command.generatorVersion().isBlank())) {
			throw new IllegalArgumentException("generator version is required for synthetic statistics");
		}
	}

	private BigDecimal priorReactionCount(RecalculateTagStatisticsCommand command) {
		return command.priorReactionCount() == null ? defaultPriorReactionCount : command.priorReactionCount();
	}
}
