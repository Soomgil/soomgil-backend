package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.UserPreferenceTagScoreRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 마이페이지 여행 취향 분석 조회용 SQL mapper.
 */
@Mapper
public interface PreferenceUserPreferenceMapper {

	/**
	 * 사용자의 태그별 선호도 점수와 태그 메타데이터를 preference_score 역순으로 조회한다.
	 */
	List<UserPreferenceTagScoreRow> findUserPreferenceScores(@Param("userId") String userId);

	/**
	 * 사용자가 스와이프 반응 이력을 가지고 있는지 확인한다 (학습 여부 판단).
	 */
	boolean hasAnyReaction(@Param("userId") String userId);
}
