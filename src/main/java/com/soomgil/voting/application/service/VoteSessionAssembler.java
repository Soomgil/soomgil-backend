package com.soomgil.voting.application.service;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.voting.api.dto.MyVoteParticipation;
import com.soomgil.voting.api.dto.TripVoteCandidate;
import com.soomgil.voting.api.dto.TripVoteParticipantSummary;
import com.soomgil.voting.api.dto.TripVoteSessionDetail;
import com.soomgil.voting.api.dto.VoteStickerPlacement;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 투표 세션 record를 API 응답 DTO로 조립한다.
 *
 * <p>진행 중인 세션에서는 후보별 스티커 집계를 노출하지 않는다. 투표 도중 중간 집계가 보이면
 * 다른 참여자의 선택에 영향을 주기 때문이며, 종료된 세션에서만 집계를 채운다.
 */
@Component
public class VoteSessionAssembler {

	/**
	 * 세션과 후보를 상세 응답으로 조립한다.
	 *
	 * @param session 세션 record
	 * @param candidates 후보 목록
	 * @param participants 참여자 목록
	 * @return 세션 상세
	 */
	public TripVoteSessionDetail toDetail(
		VoteSessionRecord session,
		List<VoteCandidateRecord> candidates,
		List<VoteParticipantRecord> participants
	) {
		boolean completed = session.status() == VoteSessionStatus.COMPLETED;
		List<TripVoteCandidate> candidateViews = candidates.stream()
			.map(candidate -> new TripVoteCandidate(
				candidate.id(),
				candidate.sortOrder(),
				toProvider(candidate.placeProvider()),
				candidate.externalPlaceId(),
				candidate.placeName(),
				candidate.address(),
				candidate.lat(),
				candidate.lng(),
				toUri(candidate.thumbnailUrl()),
				candidate.category(),
				completed ? candidate.stickerCount() : null
			))
			.toList();

		int submitted = (int) participants.stream()
			.filter(participant -> participant.submittedAt() != null)
			.count();

		return new TripVoteSessionDetail(
			session.id(),
			session.tripId(),
			session.status(),
			session.stickerAllowance(),
			session.selectionCount(),
			session.candidateCount(),
			toOffsetDateTime(session.openedAt()),
			toOffsetDateTime(session.completedAt()),
			session.completionReason(),
			new TripVoteParticipantSummary(participants.size(), submitted),
			candidateViews
		);
	}

	/**
	 * 참여자와 스티커 배치를 내 참여 상태 응답으로 조립한다.
	 *
	 * @param participant 참여자 record. 참여자가 아니면 null
	 * @param stickers 참여자의 스티커 배치
	 * @return 내 참여 상태. 참여자가 아니면 null
	 */
	public MyVoteParticipation toParticipation(
		VoteParticipantRecord participant,
		List<VoteStickerRecord> stickers
	) {
		if (participant == null) {
			return null;
		}
		List<VoteStickerPlacement> placements = stickers.stream()
			.map(sticker -> new VoteStickerPlacement(sticker.candidateId(), sticker.stickerCount()))
			.toList();
		int used = stickers.stream().mapToInt(VoteStickerRecord::stickerCount).sum();
		return new MyVoteParticipation(
			participant.id(),
			participant.status(),
			participant.stickerAllowance(),
			used,
			Math.max(0, participant.stickerAllowance() - used),
			placements,
			toOffsetDateTime(participant.submittedAt())
		);
	}

	private PlaceProvider toProvider(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return PlaceProvider.valueOf(value);
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}

	private OffsetDateTime toOffsetDateTime(Instant instant) {
		return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
