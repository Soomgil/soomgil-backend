package com.soomgil.voting.application.service;

import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.voting.application.command.dto.VoteStickerPlacementCommand;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.policy.VoteSessionPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 스티커 배치 저장 규칙을 담당하는 서비스.
 *
 * <p>배치는 부분 변경이 아니라 전체 치환이다. 목록에서 빠진 후보는 스티커를 회수한 것으로 처리하므로
 * 이동과 회수를 별도 API 없이 표현할 수 있다.
 *
 * <p>검증 순서는 제출 여부 → 후보 소속 → 개수 유효성 → 지급량 초과 여부다.
 * 제출을 마친 참여자의 저장 요청은 {@code VOTE_ALREADY_SUBMITTED}로 거절한다.
 */
@Service
public class VoteStickerPlacementService {

	private final VoteSessionRepository repository;

	public VoteStickerPlacementService(VoteSessionRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	/**
	 * 참여자의 스티커 배치를 전체 치환한다.
	 *
	 * @param session 대상 세션
	 * @param participant 대상 참여자
	 * @param placements 저장할 배치 전체
	 * @param now 저장 시각
	 * @return 저장 후 사용한 스티커 총합
	 * @throws BusinessException 이미 제출했거나 배치가 규칙을 위반한 경우
	 */
	public int replacePlacements(
		VoteSessionRecord session,
		VoteParticipantRecord participant,
		List<VoteStickerPlacementCommand> placements,
		Instant now
	) {
		if (participant.status() == VoteParticipantStatus.SUBMITTED) {
			throw new BusinessException(
				ErrorCode.VOTE_ALREADY_SUBMITTED, "Vote was already submitted and cannot be changed."
			);
		}

		Map<UUID, Integer> merged = mergeAndValidate(session, placements);
		int usedStickerCount = merged.values().stream().mapToInt(Integer::intValue).sum();
		if (!VoteSessionPolicy.isWithinAllowance(usedStickerCount, participant.stickerAllowance())) {
			throw new BusinessException(
				ErrorCode.VOTE_STICKER_ALLOWANCE_EXCEEDED,
				"Used " + usedStickerCount + " stickers but only " + participant.stickerAllowance() + " are granted."
			);
		}

		repository.deleteStickersByParticipant(participant.id());
		List<VoteStickerRecord> stickers = new ArrayList<>();
		for (Map.Entry<UUID, Integer> entry : merged.entrySet()) {
			stickers.add(new VoteStickerRecord(
				Ids.newUuid(), session.id(), participant.id(), entry.getKey(), entry.getValue()
			));
		}
		repository.insertStickers(stickers);
		repository.updateParticipantProgress(
			participant.id(),
			usedStickerCount > 0 ? VoteParticipantStatus.IN_PROGRESS : VoteParticipantStatus.NOT_STARTED,
			usedStickerCount,
			now
		);
		return usedStickerCount;
	}

	private Map<UUID, Integer> mergeAndValidate(
		VoteSessionRecord session,
		List<VoteStickerPlacementCommand> placements
	) {
		Map<UUID, Integer> merged = new LinkedHashMap<>();
		if (placements == null || placements.isEmpty()) {
			return merged;
		}

		Set<UUID> candidateIds = repository.findCandidates(session.id()).stream()
			.map(VoteCandidateRecord::id)
			.collect(Collectors.toSet());

		for (VoteStickerPlacementCommand placement : placements) {
			if (placement.candidateId() == null || !candidateIds.contains(placement.candidateId())) {
				throw new BusinessException(
					ErrorCode.VOTE_CANDIDATE_NOT_FOUND, "Candidate does not belong to this vote session."
				);
			}
			if (!VoteSessionPolicy.isValidPlacementCount(placement.stickerCount())) {
				throw new BusinessException(
					ErrorCode.VALIDATION_FAILED, "Sticker count must be greater than or equal to 1."
				);
			}
			merged.merge(placement.candidateId(), placement.stickerCount(), Integer::sum);
		}
		return merged;
	}
}
