package com.soomgil.voting.application.port;

import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 투표 세션 persistence 계약.
 *
 * <p>동시성이 걸린 두 지점은 조건부 UPDATE의 반환값으로 판정한다.
 * {@link #completeIfOpen}과 {@link #markResultAppliedIfAbsent}는 여러 요청이 동시에 들어와도
 * 정확히 한 번만 {@code true}를 반환해야 한다. 구현체는 이를 SQL의 WHERE 조건으로 보장한다.
 */
public interface VoteSessionRepository {

	/**
	 * 여행방의 진행 중(DRAFT/OPEN) 세션을 조회한다.
	 *
	 * @param tripId 여행방 식별자
	 * @return 진행 중 세션. 없으면 empty
	 */
	Optional<VoteSessionRecord> findActiveByTripId(UUID tripId);

	/**
	 * 여행방의 가장 최근 세션을 조회한다. 완료된 세션도 포함한다.
	 *
	 * @param tripId 여행방 식별자
	 * @return 최근 세션. 없으면 empty
	 */
	Optional<VoteSessionRecord> findLatestByTripId(UUID tripId);

	/**
	 * 세션을 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 세션. 없으면 empty
	 */
	Optional<VoteSessionRecord> findById(UUID sessionId);

	/**
	 * 세션 row에 잠금을 걸고 조회한다. 스티커 집계를 직렬화할 때 사용한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 잠금이 걸린 세션. 없으면 empty
	 */
	Optional<VoteSessionRecord> findByIdForUpdate(UUID sessionId);

	/**
	 * 세션을 저장한다.
	 *
	 * @param session 저장할 세션
	 */
	void insertSession(VoteSessionRecord session);

	/**
	 * 세션이 아직 OPEN일 때만 COMPLETED로 전이시킨다.
	 *
	 * <p>마지막 제출 자동 종료와 방장 조기 종료가 동시에 실행되어도 한 번만 종료되도록 하는 근거다.
	 * 호출자는 {@code true}를 받은 경우에만 결과 확정 절차를 진행해야 한다.
	 *
	 * @param sessionId 세션 식별자
	 * @param reason 종료 사유
	 * @param completedByUserId 조기 종료를 실행한 방장. 자동 종료면 null
	 * @param now 종료 시각
	 * @return 이번 호출이 실제로 종료를 수행했으면 true
	 */
	boolean completeIfOpen(
		UUID sessionId,
		VoteCompletionReason reason,
		UUID completedByUserId,
		Instant now
	);

	/**
	 * 결과 반영 표시를 아직 하지 않았을 때만 표시한다.
	 *
	 * <p>일정 추가와 취향 반영을 재시도해도 중복되지 않게 하는 근거다.
	 *
	 * @param sessionId 세션 식별자
	 * @param now 반영 시각
	 * @return 이번 호출이 실제로 표시를 수행했으면 true
	 */
	boolean markResultAppliedIfAbsent(UUID sessionId, Instant now);

	/**
	 * 후보 snapshot을 저장한다.
	 *
	 * @param candidates 저장할 후보 목록
	 */
	void insertCandidates(List<VoteCandidateRecord> candidates);

	/**
	 * 세션의 후보를 snapshot 순서대로 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 후보 목록
	 */
	List<VoteCandidateRecord> findCandidates(UUID sessionId);

	/**
	 * 후보의 스티커 집계를 갱신한다.
	 *
	 * @param candidateId 후보 식별자
	 * @param stickerCount 새 스티커 총합
	 */
	void updateCandidateTally(UUID candidateId, int stickerCount);

	/**
	 * 후보의 선정 결과를 기록한다.
	 *
	 * @param candidateId 후보 식별자
	 * @param selected 선정 여부
	 * @param selectedRank 선정 순위. 선정되지 않았으면 null
	 */
	void updateCandidateSelection(UUID candidateId, boolean selected, Integer selectedRank);

	/**
	 * 참여자를 저장한다.
	 *
	 * @param participants 저장할 참여자 목록
	 */
	void insertParticipants(List<VoteParticipantRecord> participants);

	/**
	 * 세션의 참여자 전체를 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 참여자 목록
	 */
	List<VoteParticipantRecord> findParticipants(UUID sessionId);

	/**
	 * 특정 사용자의 참여 정보를 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @param userId 사용자 식별자
	 * @return 참여 정보. 참여자가 아니면 empty
	 */
	Optional<VoteParticipantRecord> findParticipant(UUID sessionId, UUID userId);

	/**
	 * 참여 진행 상태와 사용 스티커 수를 갱신한다. 제출 전 저장에만 사용한다.
	 *
	 * @param participantId 참여자 식별자
	 * @param status 새 참여 상태
	 * @param usedStickerCount 사용한 스티커 총합
	 * @param now 갱신 시각
	 */
	void updateParticipantProgress(
		UUID participantId,
		VoteParticipantStatus status,
		int usedStickerCount,
		Instant now
	);

	/**
	 * 아직 제출하지 않은 참여자만 SUBMITTED로 전이시킨다.
	 *
	 * <p>이중 제출을 막는 근거다. 이미 제출한 참여자에 대해서는 {@code false}를 반환한다.
	 *
	 * @param participantId 참여자 식별자
	 * @param usedStickerCount 제출 시점의 사용 스티커 총합
	 * @param now 제출 시각
	 * @return 이번 호출이 실제로 제출을 기록했으면 true
	 */
	boolean markParticipantSubmittedIfNotYet(UUID participantId, int usedStickerCount, Instant now);

	/**
	 * 제출을 마친 참여자 수를 센다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 제출 완료 참여자 수
	 */
	int countSubmittedParticipants(UUID sessionId);

	/**
	 * 참여자의 기존 스티커 배치를 모두 지운다. 제출 전 전체 교체 저장에 사용한다.
	 *
	 * @param participantId 참여자 식별자
	 */
	void deleteStickersByParticipant(UUID participantId);

	/**
	 * 스티커 배치를 저장한다.
	 *
	 * @param stickers 저장할 스티커 목록
	 */
	void insertStickers(List<VoteStickerRecord> stickers);

	/**
	 * 참여자의 현재 스티커 배치를 조회한다.
	 *
	 * @param participantId 참여자 식별자
	 * @return 스티커 목록
	 */
	List<VoteStickerRecord> findStickersByParticipant(UUID participantId);

	/**
	 * 세션 전체의 스티커 배치를 조회한다. 결과 집계와 취향 반영에 사용한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 스티커 목록
	 */
	List<VoteStickerRecord> findStickersBySession(UUID sessionId);

	/**
	 * 일정 반영 결과를 기록한다. 이미 기록이 있으면 아무것도 하지 않는다.
	 *
	 * @param link 일정 반영 기록
	 * @param now 기록 시각
	 */
	void insertItineraryLinkIfAbsent(VoteItineraryLinkRecord link, Instant now);

	/**
	 * 세션의 일정 반영 기록을 조회한다.
	 *
	 * @param sessionId 세션 식별자
	 * @return 일정 반영 기록 목록
	 */
	List<VoteItineraryLinkRecord> findItineraryLinks(UUID sessionId);
}
