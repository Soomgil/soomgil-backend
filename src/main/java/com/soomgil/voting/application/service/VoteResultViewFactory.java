package com.soomgil.voting.application.service;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.voting.api.dto.TripVoteResultItem;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteItineraryLinkRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 종료된 투표의 결과 응답을 만든다.
 *
 * <p>결과는 스티커 총합 내림차순으로 정렬하고 동점이면 후보 snapshot 순서를 유지해,
 * 같은 세션을 몇 번 조회해도 같은 순서로 보이게 한다.
 */
@Component
public class VoteResultViewFactory {

	/**
	 * 세션과 후보, 일정 반영 기록으로 결과 응답을 만든다.
	 *
	 * @param session 종료된 세션
	 * @param candidates 후보 목록
	 * @param links 후보별 일정 반영 기록
	 * @param unscheduledDayId 일정이 추가된 일차 미정 그룹. 없으면 null
	 * @param itineraryVersion 일정 반영 후 협업 version. 없으면 null
	 * @return 결과 응답
	 */
	public TripVoteSessionResult create(
		VoteSessionRecord session,
		List<VoteCandidateRecord> candidates,
		List<VoteItineraryLinkRecord> links,
		UUID unscheduledDayId,
		Long itineraryVersion
	) {
		Map<UUID, VoteItineraryLinkRecord> linkByCandidate = new LinkedHashMap<>();
		for (VoteItineraryLinkRecord link : links) {
			linkByCandidate.putIfAbsent(link.candidateId(), link);
		}

		List<TripVoteResultItem> items = candidates.stream()
			.sorted(Comparator
				.comparingInt(VoteCandidateRecord::stickerCount).reversed()
				.thenComparingInt(VoteCandidateRecord::sortOrder))
			.map(candidate -> {
				VoteItineraryLinkRecord link = linkByCandidate.get(candidate.id());
				return new TripVoteResultItem(
					candidate.id(),
					toProvider(candidate.placeProvider()),
					candidate.externalPlaceId(),
					candidate.placeName(),
					toUri(candidate.thumbnailUrl()),
					candidate.stickerCount(),
					candidate.selected(),
					candidate.selectedRank(),
					link == null ? null : link.outcome(),
					link == null ? null : link.itineraryItemId()
				);
			})
			.toList();

		return new TripVoteSessionResult(
			session.id(),
			session.tripId(),
			session.status(),
			session.completionReason(),
			session.completedAt() == null
				? null
				: OffsetDateTime.ofInstant(session.completedAt(), ZoneOffset.UTC),
			session.selectionCount(),
			items,
			unscheduledDayId,
			itineraryVersion
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
}
