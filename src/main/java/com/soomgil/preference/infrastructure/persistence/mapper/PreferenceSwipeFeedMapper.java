package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedPlaceRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * preference 스와이프 feed SQL mapper.
 */
@Mapper
public interface PreferenceSwipeFeedMapper {

	List<SwipeFeedPlaceRow> findFeed(
		@Param("userId") String userId,
		@Param("legalRegionCode") String legalRegionCode,
		@Param("category") String category,
		@Param("limit") int limit,
		@Param("excludeRecent") boolean excludeRecent
	);
}
