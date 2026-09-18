package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.OnboardingSurveyPlaceRow;
import com.soomgil.preference.infrastructure.persistence.row.OnboardingSurveyVersionRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 가입 취향 설문 version, 장소, 사용자 응답을 저장하는 MyBatis 경계.
 */
@Mapper
public interface OnboardingPreferenceMapper {

	@Select("""
		SELECT id, code, required_place_count AS requiredPlaceCount
		FROM preference.onboarding_survey_versions
		WHERE status = 'ACTIVE'
		""")
	Optional<OnboardingSurveyVersionRow> findActiveVersion();

	@Select("""
		SELECT
			place.provider,
			place.external_place_id AS externalPlaceId,
			attraction.title AS name,
			trim(concat_ws(' ', attraction.addr1, attraction.addr2)) AS address,
			attraction.first_image1 AS thumbnailUrl,
			content_type.content_type_name AS category,
			attraction.overview AS description,
			coalesce((
				SELECT string_agg(tag.display_name, '|' ORDER BY enrichment_tag.rank_order)
				FROM preference.place_tag_enrichments enrichment
				JOIN preference.place_tag_enrichment_tags enrichment_tag
					ON enrichment_tag.enrichment_id = enrichment.id
				JOIN preference.preference_tags tag ON tag.id = enrichment_tag.tag_id
				WHERE enrichment.provider = place.provider
					AND enrichment.external_place_id = place.external_place_id
					AND enrichment.status = 'SUCCEEDED'
					AND enrichment.id = (
						SELECT latest.id
						FROM preference.place_tag_enrichments latest
						WHERE latest.provider = place.provider
							AND latest.external_place_id = place.external_place_id
							AND latest.status = 'SUCCEEDED'
						ORDER BY latest.enriched_at DESC NULLS LAST, latest.created_at DESC, latest.id
						LIMIT 1
					)
			), '') AS tagLabels,
			place.sort_order AS sortOrder
		FROM preference.onboarding_survey_places place
		JOIN tourism_source.attractions attraction
			ON CAST(attraction.content_id AS varchar) = place.external_place_id
		LEFT JOIN tourism_source.contenttypes content_type
			ON content_type.content_type_id = attraction.content_type_id
		WHERE place.survey_version_id = #{surveyVersionId}
		ORDER BY place.sort_order
		""")
	List<OnboardingSurveyPlaceRow> findPlaces(@Param("surveyVersionId") UUID surveyVersionId);

	@Select("SELECT onboarding_completed_at FROM auth.users WHERE id = #{userId}")
	OffsetDateTime findCompletedAt(@Param("userId") UUID userId);

	@Insert("""
		INSERT INTO preference.user_onboarding_responses (
			user_id, survey_version_id, provider, external_place_id, reaction
		)
		VALUES (#{userId}, #{surveyVersionId}, #{provider}, #{externalPlaceId}, #{reaction})
		ON CONFLICT (user_id, survey_version_id, provider, external_place_id)
		DO UPDATE SET reaction = excluded.reaction, responded_at = now()
		""")
	void upsertResponse(
		@Param("userId") UUID userId,
		@Param("surveyVersionId") UUID surveyVersionId,
		@Param("provider") String provider,
		@Param("externalPlaceId") String externalPlaceId,
		@Param("reaction") String reaction
	);

	@Update("""
		UPDATE auth.users
		SET onboarding_completed_at = coalesce(onboarding_completed_at, now()),
			updated_at = now()
		WHERE id = #{userId}
		""")
	void markCompleted(@Param("userId") UUID userId);
}
