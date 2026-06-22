package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.SavedPlaceInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.SavedPlaceRow;
import com.soomgil.preference.infrastructure.persistence.row.PopularPlaceRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * preference 저장 장소 SQL mapper.
 */
@Mapper
public interface PreferenceSavedPlaceMapper {

	String findReaction(
		@Param("userId") String userId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	String findSavedPlaceId(
		@Param("userId") String userId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	void insertSavedPlace(SavedPlaceInsertRow row);

	void reactivateSavedPlace(@Param("savedPlaceId") String savedPlaceId);

	int softDeleteSavedPlace(
		@Param("userId") String userId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	SavedPlaceRow findSavedPlaceById(@Param("savedPlaceId") String savedPlaceId);

	long countSavedPlaces(@Param("userId") String userId);

	List<SavedPlaceRow> listSavedPlaces(
		@Param("userId") String userId,
		@Param("limit") int limit,
		@Param("offset") int offset
	);

	List<PopularPlaceRow> listPopularPlaces(@Param("limit") int limit);
}
