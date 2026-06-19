package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionUpdateRow;
import com.soomgil.preference.infrastructure.persistence.row.UserSwipeEventInsertRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * preference 스와이프 반응 SQL mapper.
 */
@Mapper
public interface PreferenceSwipeReactionMapper {

	UserPlaceReactionRow findReaction(
		@Param("userId") String userId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	void insertReaction(UserPlaceReactionInsertRow row);

	void updateReaction(UserPlaceReactionUpdateRow row);

	void insertEvent(UserSwipeEventInsertRow row);
}
