package com.soomgil.voting.infrastructure.persistence.mapper;

import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteItineraryLinkRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteStickerRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 투표 세션 SQL mapper.
 *
 * <p>{@code completeSessionIfOpen}과 {@code markResultAppliedIfAbsent}는 조건부 UPDATE의
 * 영향 row 수로 "이번 호출이 실제로 수행했는가"를 알려준다. 이 두 SQL이 동시 종료와 결과 중복 반영을
 * 막는 실제 근거이므로 WHERE 조건을 완화하면 안 된다.
 */
@Mapper
public interface VoteSessionMapper {

	/**
	 * 여행방의 진행 중 세션을 조회한다.
	 *
	 * @param tripId 여행방 식별자
	 * @return 진행 중 세션. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, status, created_by_user_id, sticker_allowance, selection_count, candidate_count,
		       opened_at, completed_at, completion_reason, completed_by_user_id, result_applied_at
		FROM voting.vote_sessions
		WHERE trip_id = #{tripId} AND status IN ('DRAFT', 'OPEN')
		""")
	Optional<VoteSessionRecord> findActiveByTripId(@Param("tripId") UUID tripId);

	/**
	 * 여행방의 가장 최근 세션을 조회한다.
	 *
	 * @param tripId 여행방 식별자
	 * @return 최근 세션. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, status, created_by_user_id, sticker_allowance, selection_count, candidate_count,
		       opened_at, completed_at, completion_reason, completed_by_user_id, result_applied_at
		FROM voting.vote_sessions
		WHERE trip_id = #{tripId}
		ORDER BY created_at DESC
		LIMIT 1
		""")
	Optional<VoteSessionRecord> findLatestByTripId(@Param("tripId") UUID tripId);

	/**
	 * 세션을 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 세션. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, status, created_by_user_id, sticker_allowance, selection_count, candidate_count,
		       opened_at, completed_at, completion_reason, completed_by_user_id, result_applied_at
		FROM voting.vote_sessions
		WHERE id = #{sessionId}
		""")
	Optional<VoteSessionRecord> findById(@Param("sessionId") UUID sessionId);

	/**
	 * 세션 row에 잠금을 걸고 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 잠금이 걸린 세션. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, status, created_by_user_id, sticker_allowance, selection_count, candidate_count,
		       opened_at, completed_at, completion_reason, completed_by_user_id, result_applied_at
		FROM voting.vote_sessions
		WHERE id = #{sessionId}
		FOR UPDATE
		""")
	Optional<VoteSessionRecord> findByIdForUpdate(@Param("sessionId") UUID sessionId);

	/**
	 * 세션을 저장한다.
	 *
	 * @param session 저장할 세션
	 */
	@Insert("""
		INSERT INTO voting.vote_sessions (
		    id, trip_id, status, created_by_user_id, sticker_allowance, selection_count, candidate_count, opened_at
		)
		VALUES (
		    #{id}, #{tripId}, #{status}, #{createdByUserId},
		    #{stickerAllowance}, #{selectionCount}, #{candidateCount}, #{openedAt}
		)
		""")
	void insertSession(VoteSessionRecord session);

	/**
	 * 세션이 아직 OPEN일 때만 종료 상태로 전이시킨다.
	 *
	 * @param sessionId 세션 식별자
	 * @param reason 종료 사유
	 * @param completedByUserId 조기 종료를 실행한 방장. 자동 종료면 null
	 * @param now 종료 시각
	 * @return 전이된 row 수. 이미 종료되어 있었으면 0
	 */
	@Update("""
		UPDATE voting.vote_sessions SET
		    status = 'COMPLETED',
		    completion_reason = #{reason},
		    completed_by_user_id = #{completedByUserId},
		    completed_at = #{now},
		    updated_at = #{now}
		WHERE id = #{sessionId} AND status = 'OPEN'
		""")
	int completeSessionIfOpen(
		@Param("sessionId") UUID sessionId,
		@Param("reason") String reason,
		@Param("completedByUserId") UUID completedByUserId,
		@Param("now") Instant now
	);

	/**
	 * 결과 반영 표시를 아직 하지 않았을 때만 표시한다.
	 *
	 * @param sessionId 세션 식별자
	 * @param now 반영 시각
	 * @return 표시된 row 수. 이미 반영되어 있었으면 0
	 */
	@Update("""
		UPDATE voting.vote_sessions SET
		    result_applied_at = #{now},
		    updated_at = #{now}
		WHERE id = #{sessionId} AND result_applied_at IS NULL
		""")
	int markResultAppliedIfAbsent(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

	/**
	 * 후보 하나를 저장한다.
	 *
	 * @param candidate 저장할 후보
	 */
	@Insert("""
		INSERT INTO voting.vote_session_candidates (
		    id, vote_session_id, sort_order, place_provider, external_place_id, place_name,
		    address, lat, lng, thumbnail_url, category, sticker_count, selected, selected_rank
		)
		VALUES (
		    #{id}, #{voteSessionId}, #{sortOrder}, #{placeProvider}, #{externalPlaceId}, #{placeName},
		    #{address}, #{lat}, #{lng}, #{thumbnailUrl}, #{category}, #{stickerCount}, #{selected}, #{selectedRank}
		)
		""")
	void insertCandidate(VoteCandidateRecord candidate);

	/**
	 * 세션의 후보를 snapshot 순서대로 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 후보 목록
	 */
	@Select("""
		SELECT id, vote_session_id, sort_order, place_provider, external_place_id, place_name,
		       address, lat, lng, thumbnail_url, category, sticker_count, selected, selected_rank
		FROM voting.vote_session_candidates
		WHERE vote_session_id = #{sessionId}
		ORDER BY sort_order
		""")
	List<VoteCandidateRecord> findCandidates(@Param("sessionId") UUID sessionId);

	/**
	 * 후보의 스티커 집계를 갱신한다.
	 *
	 * @param candidateId 후보 식별자
	 * @param stickerCount 새 스티커 총합
	 */
	@Update("""
		UPDATE voting.vote_session_candidates SET sticker_count = #{stickerCount}
		WHERE id = #{candidateId}
		""")
	void updateCandidateTally(
		@Param("candidateId") UUID candidateId,
		@Param("stickerCount") int stickerCount
	);

	/**
	 * 후보의 선정 결과를 기록한다.
	 *
	 * @param candidateId 후보 식별자
	 * @param selected 선정 여부
	 * @param selectedRank 선정 순위
	 */
	@Update("""
		UPDATE voting.vote_session_candidates SET selected = #{selected}, selected_rank = #{selectedRank}
		WHERE id = #{candidateId}
		""")
	void updateCandidateSelection(
		@Param("candidateId") UUID candidateId,
		@Param("selected") boolean selected,
		@Param("selectedRank") Integer selectedRank
	);

	/**
	 * 참여자 하나를 저장한다.
	 *
	 * @param participant 저장할 참여자
	 */
	@Insert("""
		INSERT INTO voting.vote_session_participants (
		    id, vote_session_id, user_id, status, sticker_allowance, used_sticker_count, submitted_at
		)
		VALUES (
		    #{id}, #{voteSessionId}, #{userId}, #{status}, #{stickerAllowance}, #{usedStickerCount}, #{submittedAt}
		)
		""")
	void insertParticipant(VoteParticipantRecord participant);

	/**
	 * 세션의 참여자 전체를 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 참여자 목록
	 */
	@Select("""
		SELECT id, vote_session_id, user_id, status, sticker_allowance, used_sticker_count, submitted_at
		FROM voting.vote_session_participants
		WHERE vote_session_id = #{sessionId}
		ORDER BY created_at
		""")
	List<VoteParticipantRecord> findParticipants(@Param("sessionId") UUID sessionId);

	/**
	 * 특정 사용자의 참여 정보를 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @param userId 사용자 식별자
	 * @return 참여 정보. 참여자가 아니면 empty
	 */
	@Select("""
		SELECT id, vote_session_id, user_id, status, sticker_allowance, used_sticker_count, submitted_at
		FROM voting.vote_session_participants
		WHERE vote_session_id = #{sessionId} AND user_id = #{userId}
		""")
	Optional<VoteParticipantRecord> findParticipant(
		@Param("sessionId") UUID sessionId,
		@Param("userId") UUID userId
	);

	/**
	 * 참여 진행 상태를 갱신한다.
	 *
	 * @param participantId 참여자 식별자
	 * @param status 새 참여 상태
	 * @param usedStickerCount 사용한 스티커 총합
	 * @param now 갱신 시각
	 */
	@Update("""
		UPDATE voting.vote_session_participants SET
		    status = #{status},
		    used_sticker_count = #{usedStickerCount},
		    updated_at = #{now}
		WHERE id = #{participantId}
		""")
	void updateParticipantProgress(
		@Param("participantId") UUID participantId,
		@Param("status") String status,
		@Param("usedStickerCount") int usedStickerCount,
		@Param("now") Instant now
	);

	/**
	 * 아직 제출하지 않은 참여자만 SUBMITTED로 전이시킨다.
	 *
	 * @param participantId 참여자 식별자
	 * @param usedStickerCount 제출 시점 사용 스티커 총합
	 * @param now 제출 시각
	 * @return 전이된 row 수. 이미 제출되어 있었으면 0
	 */
	@Update("""
		UPDATE voting.vote_session_participants SET
		    status = 'SUBMITTED',
		    used_sticker_count = #{usedStickerCount},
		    submitted_at = #{now},
		    updated_at = #{now}
		WHERE id = #{participantId} AND status <> 'SUBMITTED'
		""")
	int markParticipantSubmittedIfNotYet(
		@Param("participantId") UUID participantId,
		@Param("usedStickerCount") int usedStickerCount,
		@Param("now") Instant now
	);

	/**
	 * 제출을 마친 참여자 수를 센다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 제출 완료 참여자 수
	 */
	@Select("""
		SELECT COUNT(*)
		FROM voting.vote_session_participants
		WHERE vote_session_id = #{sessionId} AND status = 'SUBMITTED'
		""")
	int countSubmittedParticipants(@Param("sessionId") UUID sessionId);

	/**
	 * 참여자의 스티커 배치를 모두 지운다.
	 *
	 * @param participantId 참여자 식별자
	 */
	@Delete("DELETE FROM voting.vote_stickers WHERE participant_id = #{participantId}")
	void deleteStickersByParticipant(@Param("participantId") UUID participantId);

	/**
	 * 스티커 배치 하나를 저장한다.
	 *
	 * @param sticker 저장할 배치
	 */
	@Insert("""
		INSERT INTO voting.vote_stickers (id, vote_session_id, participant_id, candidate_id, sticker_count)
		VALUES (#{id}, #{voteSessionId}, #{participantId}, #{candidateId}, #{stickerCount})
		""")
	void insertSticker(VoteStickerRecord sticker);

	/**
	 * 참여자의 스티커 배치를 조회한다.
	 *
	 * @param participantId 참여자 식별자
	 * @return 스티커 목록
	 */
	@Select("""
		SELECT id, vote_session_id, participant_id, candidate_id, sticker_count
		FROM voting.vote_stickers
		WHERE participant_id = #{participantId}
		""")
	List<VoteStickerRecord> findStickersByParticipant(@Param("participantId") UUID participantId);

	/**
	 * 세션 전체의 스티커 배치를 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 스티커 목록
	 */
	@Select("""
		SELECT id, vote_session_id, participant_id, candidate_id, sticker_count
		FROM voting.vote_stickers
		WHERE vote_session_id = #{sessionId}
		""")
	List<VoteStickerRecord> findStickersBySession(@Param("sessionId") UUID sessionId);

	/**
	 * 일정 반영 기록을 저장한다. 이미 있으면 아무것도 하지 않는다.
	 *
	 * @param link 일정 반영 기록
	 * @param now 기록 시각
	 */
	@Insert("""
		INSERT INTO voting.vote_session_itinerary_links (
		    vote_session_id, candidate_id, itinerary_item_id, outcome, created_at
		)
		VALUES (#{link.voteSessionId}, #{link.candidateId}, #{link.itineraryItemId}, #{link.outcome}, #{now})
		ON CONFLICT (vote_session_id, candidate_id) DO NOTHING
		""")
	void insertItineraryLinkIfAbsent(@Param("link") VoteItineraryLinkRecord link, @Param("now") Instant now);

	/**
	 * 세션의 일정 반영 기록을 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 일정 반영 기록 목록
	 */
	@Select("""
		SELECT vote_session_id, candidate_id, itinerary_item_id, outcome
		FROM voting.vote_session_itinerary_links
		WHERE vote_session_id = #{sessionId}
		""")
	List<VoteItineraryLinkRecord> findItineraryLinks(@Param("sessionId") UUID sessionId);
}
