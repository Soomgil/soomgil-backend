package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.infrastructure.persistence.row.UserPlaceVoteEvidenceInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagVoteEvidenceRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * TRIP_VOTE 취향 근거 SQL mapper.
 *
 * <p>스와이프 반응 테이블은 건드리지 않고 별도 근거 테이블과 projection 가산만 담당한다.
 */
@Mapper
public interface PreferenceTripVoteMapper {

	/**
	 * 투표 취향 근거를 기록한다. 같은 (세션, 사용자, 장소)가 이미 있으면 아무것도 하지 않는다.
	 *
	 * <p>반환값이 0이면 이미 반영된 제출이므로 호출자는 태그 근거 가산을 건너뛰어야 한다.
	 * 이 동작이 재시도 멱등성의 근거다.
	 *
	 * @param row 근거 감사 row
	 * @return 실제로 INSERT된 row 수. 이미 반영되어 있으면 0
	 */
	@Insert("""
		INSERT INTO preference.user_place_vote_evidences (
		    id, vote_session_id, user_id, provider, external_place_id,
		    sticker_count, source, place_tag_enrichment_id, evidence_units, calculation_version
		)
		VALUES (
		    CAST(#{id} AS uuid),
		    CAST(#{voteSessionId} AS uuid),
		    CAST(#{userId} AS uuid),
		    #{provider},
		    #{externalPlaceId},
		    #{stickerCount},
		    #{source},
		    CAST(#{placeTagEnrichmentId} AS uuid),
		    #{evidenceUnits},
		    #{calculationVersion}
		)
		ON CONFLICT (vote_session_id, user_id, provider, external_place_id) DO NOTHING
		""")
	int insertEvidenceIfAbsent(UserPlaceVoteEvidenceInsertRow row);

	/**
	 * 사용자 태그 projection에 투표 근거를 가산한다.
	 *
	 * <p>{@code positive_evidence}에도 같은 값을 더해 기존 추천 점수 계산이 그대로 동작하게 하고,
	 * {@code vote_evidence}와 {@code vote_place_count}에는 투표 기여분을 따로 누적해 감사할 수 있게 한다.
	 * 스와이프 반응 횟수 컬럼({@code like_count}, {@code super_like_count}, {@code nope_count})은
	 * 건드리지 않는다.
	 *
	 * @param row 가산할 사용자/태그/근거
	 */
	@Insert("""
		INSERT INTO preference.user_preference_tag_weights (
		    user_id, tag_id, positive_evidence, negative_evidence, preference_score,
		    like_count, super_like_count, nope_count, calculation_version,
		    vote_evidence, vote_place_count
		)
		VALUES (
		    CAST(#{userId} AS uuid),
		    CAST(#{tagId} AS uuid),
		    #{evidence},
		    0,
		    0.5,
		    0,
		    0,
		    0,
		    'preference-evidence-v1',
		    #{evidence},
		    1
		)
		ON CONFLICT (user_id, tag_id)
		DO UPDATE SET
		    positive_evidence = preference.user_preference_tag_weights.positive_evidence + EXCLUDED.positive_evidence,
		    vote_evidence = preference.user_preference_tag_weights.vote_evidence + EXCLUDED.vote_evidence,
		    vote_place_count = preference.user_preference_tag_weights.vote_place_count + 1,
		    updated_at = now()
		""")
	void addUserTagVoteEvidence(UserTagVoteEvidenceRow row);
}
