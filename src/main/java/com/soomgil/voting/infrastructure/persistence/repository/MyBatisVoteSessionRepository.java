package com.soomgil.voting.infrastructure.persistence.repository;

import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteItineraryLinkRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.infrastructure.persistence.mapper.VoteSessionMapper;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 투표 세션 저장소.
 *
 * <p>동시성 판정이 필요한 두 연산은 mapper의 조건부 UPDATE 반환값을 그대로 boolean으로 옮긴다.
 * 여기서 반환값을 무시하면 동시 종료와 결과 중복 반영을 막을 수 없다.
 */
@Repository
public class MyBatisVoteSessionRepository implements VoteSessionRepository {

	private final VoteSessionMapper mapper;

	public MyBatisVoteSessionRepository(VoteSessionMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public Optional<VoteSessionRecord> findActiveByTripId(UUID tripId) {
		return mapper.findActiveByTripId(tripId);
	}

	@Override
	public Optional<VoteSessionRecord> findLatestByTripId(UUID tripId) {
		return mapper.findLatestByTripId(tripId);
	}

	@Override
	public Optional<VoteSessionRecord> findById(UUID sessionId) {
		return mapper.findById(sessionId);
	}

	@Override
	public Optional<VoteSessionRecord> findByIdForUpdate(UUID sessionId) {
		return mapper.findByIdForUpdate(sessionId);
	}

	@Override
	public void insertSession(VoteSessionRecord session) {
		mapper.insertSession(session);
	}

	@Override
	public boolean completeIfOpen(
		UUID sessionId,
		VoteCompletionReason reason,
		UUID completedByUserId,
		Instant now
	) {
		return mapper.completeSessionIfOpen(
			sessionId, reason == null ? null : reason.name(), completedByUserId, now
		) > 0;
	}

	@Override
	public boolean markResultAppliedIfAbsent(UUID sessionId, Instant now) {
		return mapper.markResultAppliedIfAbsent(sessionId, now) > 0;
	}

	@Override
	public void insertCandidates(List<VoteCandidateRecord> candidates) {
		for (VoteCandidateRecord candidate : candidates) {
			mapper.insertCandidate(candidate);
		}
	}

	@Override
	public List<VoteCandidateRecord> findCandidates(UUID sessionId) {
		return mapper.findCandidates(sessionId);
	}

	@Override
	public void updateCandidateTally(UUID candidateId, int stickerCount) {
		mapper.updateCandidateTally(candidateId, stickerCount);
	}

	@Override
	public void updateCandidateSelection(UUID candidateId, boolean selected, Integer selectedRank) {
		mapper.updateCandidateSelection(candidateId, selected, selectedRank);
	}

	@Override
	public void insertParticipants(List<VoteParticipantRecord> participants) {
		for (VoteParticipantRecord participant : participants) {
			mapper.insertParticipant(participant);
		}
	}

	@Override
	public List<VoteParticipantRecord> findParticipants(UUID sessionId) {
		return mapper.findParticipants(sessionId);
	}

	@Override
	public Optional<VoteParticipantRecord> findParticipant(UUID sessionId, UUID userId) {
		return mapper.findParticipant(sessionId, userId);
	}

	@Override
	public void updateParticipantProgress(
		UUID participantId,
		VoteParticipantStatus status,
		int usedStickerCount,
		Instant now
	) {
		mapper.updateParticipantProgress(participantId, status.name(), usedStickerCount, now);
	}

	@Override
	public boolean markParticipantSubmittedIfNotYet(UUID participantId, int usedStickerCount, Instant now) {
		return mapper.markParticipantSubmittedIfNotYet(participantId, usedStickerCount, now) > 0;
	}

	@Override
	public int countSubmittedParticipants(UUID sessionId) {
		return mapper.countSubmittedParticipants(sessionId);
	}

	@Override
	public void deleteStickersByParticipant(UUID participantId) {
		mapper.deleteStickersByParticipant(participantId);
	}

	@Override
	public void insertStickers(List<VoteStickerRecord> stickers) {
		for (VoteStickerRecord sticker : stickers) {
			mapper.insertSticker(sticker);
		}
	}

	@Override
	public List<VoteStickerRecord> findStickersByParticipant(UUID participantId) {
		return mapper.findStickersByParticipant(participantId);
	}

	@Override
	public List<VoteStickerRecord> findStickersBySession(UUID sessionId) {
		return mapper.findStickersBySession(sessionId);
	}

	@Override
	public void insertItineraryLinkIfAbsent(VoteItineraryLinkRecord link, Instant now) {
		mapper.insertItineraryLinkIfAbsent(link, now);
	}

	@Override
	public List<VoteItineraryLinkRecord> findItineraryLinks(UUID sessionId) {
		return mapper.findItineraryLinks(sessionId);
	}
}
