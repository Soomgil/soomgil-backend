package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * preference 스와이프 feed SQL mapper.
 */
@Mapper
public interface PreferenceSwipeFeedMapper {

	List<SwipeFeedReactionRow> findReactions(
		@Param("userId") String userId,
		@Param("externalPlaceIds") List<String> externalPlaceIds
	);

	List<SwipeFeedTagRow> findTags(@Param("externalPlaceIds") List<String> externalPlaceIds);
}
