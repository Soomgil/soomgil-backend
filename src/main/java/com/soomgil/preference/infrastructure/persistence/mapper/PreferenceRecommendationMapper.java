package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.preference.infrastructure.persistence.row.RecommendationScoreSourceRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 여행 멤버와 viewport 장소의 추천 계산 원천을 한 번에 조회하는 MyBatis mapper.
 */
@Mapper
public interface PreferenceRecommendationMapper {

	List<RecommendationScoreSourceRow> findScoreSources(
		@Param("memberIds") List<String> memberIds,
		@Param("places") List<PlaceRef> places
	);
}
