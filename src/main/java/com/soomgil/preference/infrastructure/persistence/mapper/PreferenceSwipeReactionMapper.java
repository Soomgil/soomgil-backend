package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionUpdateRow;
import com.soomgil.preference.infrastructure.persistence.row.UserSwipeEventInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEvidenceSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagEvidenceAdjustmentRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreUpdateRow;
import com.soomgil.preference.infrastructure.persistence.row.SwipeReactionRefreshRow;
import java.time.OffsetDateTime;
import java.util.List;
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

	void upsertSuperLikeSavedPlace(
		@Param("id") String id,
		@Param("userId") String userId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	void removeSavedPlaceForNonSuperLike(
		@Param("userId") String userId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	List<PlaceTagEvidenceSourceRow> findLatestConfirmedTags(
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId
	);

	List<PlaceTagEvidenceSourceRow> findConfirmedTagsByEnrichment(
		@Param("enrichmentId") String enrichmentId
	);

	void removeUserTagEvidence(UserTagEvidenceAdjustmentRow row);

	void addUserTagEvidence(UserTagEvidenceAdjustmentRow row);

	UserTagPreferenceScoreSourceRow findUserTagPreferenceScoreSource(
		@Param("userId") String userId,
		@Param("tagId") String tagId
	);

	void updateUserTagPreferenceScore(UserTagPreferenceScoreUpdateRow row);

	List<SwipeReactionRefreshRow> findReactionsNeedingEnrichmentRefresh(
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId,
		@Param("enrichmentId") String enrichmentId
	);

	void updateReactionEnrichment(
		@Param("id") String id,
		@Param("enrichmentId") String enrichmentId,
		@Param("sourceModifiedAt") OffsetDateTime sourceModifiedAt
	);
}
