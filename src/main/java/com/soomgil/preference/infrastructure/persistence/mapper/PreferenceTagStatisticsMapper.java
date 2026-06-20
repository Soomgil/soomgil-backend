package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.TagReactionAggregateRow;
import com.soomgil.preference.infrastructure.persistence.row.TagStatisticInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.TagStatisticRunInsertRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 최종 사용자 반응 집계와 태그 통계 저장을 담당하는 MyBatis mapper.
 */
@Mapper
public interface PreferenceTagStatisticsMapper {

	long countFinalReactions();

	long countPositiveFinalReactions();

	List<TagReactionAggregateRow> aggregateFinalReactionsByTag();

	long countSyntheticReactions(@Param("generatorVersion") String generatorVersion);

	long countPositiveSyntheticReactions(@Param("generatorVersion") String generatorVersion);

	List<TagReactionAggregateRow> aggregateSyntheticReactionsByTag(
		@Param("generatorVersion") String generatorVersion
	);

	long countActiveSyntheticPersonas(@Param("generatorVersion") String generatorVersion);

	long countActiveSyntheticPersonasWithoutEvents(@Param("generatorVersion") String generatorVersion);

	void insertRun(TagStatisticRunInsertRow row);

	void insertStatistic(TagStatisticInsertRow row);

	void deactivateServingRuns();

	void completeAndServeRun(@Param("runId") String runId);
}
