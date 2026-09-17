package com.soomgil.voting.application.service;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledCommand;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledResult;
import com.soomgil.itinerary.application.command.dto.AddedUnscheduledPlace;
import com.soomgil.itinerary.application.command.dto.SkippedUnscheduledPlace;
import com.soomgil.itinerary.application.command.dto.UnscheduledPlaceToAdd;
import com.soomgil.itinerary.application.command.handler.AddPlacesToUnscheduledHandler;
import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceCommand;
import com.soomgil.preference.application.command.dto.TripVoteStickerPlace;
import com.soomgil.preference.application.command.handler.ApplyTripVotePreferenceCommandHandler;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteItineraryLinkRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.port.VoteStickerRecord;
import com.soomgil.voting.domain.model.VoteCompletionReason;
import com.soomgil.voting.domain.policy.VoteCandidateTally;
import com.soomgil.voting.domain.policy.VoteResultSelectionPolicy;
import java.net.URI;
import java.time.Instant;
import com.soomgil.voting.application.port.VoteNotificationPublisher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 투표 종료와 결과 확정을 담당하는 공통 서비스.
 *
 * <p>전원 제출 자동 종료와 방장 조기 종료는 모두 이 서비스를 통과한다. 종료 전이는
 * {@code UPDATE ... WHERE status = 'OPEN'} 한 번으로만 일어나므로 두 경로가 동시에 실행되어도
 * 실제 종료는 한 번만 수행된다.
 *
 * <p>결과 확정도 {@code result_applied_at}이 비어 있을 때만 진행하므로 재시도해도 일정과 취향이
 * 중복 반영되지 않는다. 일정 추가는 itinerary의 공개 command, 취향 반영은 preference의 공개 command만
 * 호출하고 두 모듈의 DB/mapper에는 접근하지 않는다.
 */
@Service
public class CompleteVoteSessionService {

	private static final String OUTCOME_ADDED = "ADDED";
	private static final String OUTCOME_SKIPPED_DUPLICATE = "SKIPPED_DUPLICATE";

	private final VoteSessionRepository repository;
	private final AddPlacesToUnscheduledHandler addPlacesToUnscheduledHandler;
	private final ApplyTripVotePreferenceCommandHandler applyTripVotePreferenceCommandHandler;
	private final VoteResultSelectionPolicy selectionPolicy;
	private final TimeProvider timeProvider;
	private final VoteNotificationPublisher notifications;

	public CompleteVoteSessionService(
		VoteSessionRepository repository,
		AddPlacesToUnscheduledHandler addPlacesToUnscheduledHandler,
		ApplyTripVotePreferenceCommandHandler applyTripVotePreferenceCommandHandler,
		TimeProvider timeProvider,
		VoteNotificationPublisher notifications
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.addPlacesToUnscheduledHandler =
			Objects.requireNonNull(addPlacesToUnscheduledHandler, "addPlacesToUnscheduledHandler must not be null");
		this.applyTripVotePreferenceCommandHandler = Objects.requireNonNull(
			applyTripVotePreferenceCommandHandler, "applyTripVotePreferenceCommandHandler must not be null"
		);
		this.selectionPolicy = new VoteResultSelectionPolicy();
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
	}

	/**
	 * 세션을 종료하고 결과를 확정한다.
	 *
	 * <p>이미 종료된 세션이면 종료 전이는 건너뛰고 기존 결과를 그대로 반환한다.
	 *
	 * @param session 대상 세션
	 * @param reason 종료 사유
	 * @param completedByUserId 조기 종료를 실행한 방장. 자동 종료면 null
	 * @return 확정된 결과 요약
	 */
	public CompletionOutcome complete(
		VoteSessionRecord session,
		VoteCompletionReason reason,
		UUID completedByUserId
	) {
		Instant now = timeProvider.now();
		repository.completeIfOpen(session.id(), reason, completedByUserId, now);
		return applyResultOnce(session, now);
	}

	/**
	 * 결과가 아직 반영되지 않았을 때만 선정, 일정 추가, 취향 반영을 수행한다.
	 *
	 * @param session 대상 세션
	 * @param now 처리 시각
	 * @return 확정된 결과 요약
	 */
	public CompletionOutcome applyResultOnce(VoteSessionRecord session, Instant now) {
		if (!repository.markResultAppliedIfAbsent(session.id(), now)) {
			return existingOutcome(session);
		}

		List<VoteCandidateRecord> candidates = repository.findCandidates(session.id());
		List<VoteStickerRecord> stickers = repository.findStickersBySession(session.id());
		Map<UUID, Integer> stickerCountByCandidate = stickers.stream().collect(Collectors.groupingBy(
			VoteStickerRecord::candidateId,
			Collectors.summingInt(VoteStickerRecord::stickerCount)
		));

		for (VoteCandidateRecord candidate : candidates) {
			repository.updateCandidateTally(
				candidate.id(),
				stickerCountByCandidate.getOrDefault(candidate.id(), 0)
			);
		}

		List<VoteCandidateTally> selected = selectionPolicy.select(
			candidates.stream()
				.map(candidate -> new VoteCandidateTally(
					candidate.id(),
					candidate.sortOrder(),
					stickerCountByCandidate.getOrDefault(candidate.id(), 0),
					false,
					null
				))
				.toList(),
			session.selectionCount()
		);
		for (VoteCandidateTally tally : selected) {
			repository.updateCandidateSelection(tally.candidateId(), true, tally.selectedRank());
		}

		AddPlacesToUnscheduledResult itineraryResult =
			addSelectedPlacesToItinerary(session, candidates, selected, now);
		applyPreferences(session, stickers);
		notifications.publish(session.tripId(), session.id(), true, now);

		return new CompletionOutcome(
			stickerCountByCandidate,
			selected,
			itineraryResult == null ? null : itineraryResult.unscheduledDayId(),
			itineraryResult == null ? null : itineraryResult.itineraryVersion(),
			repository.findItineraryLinks(session.id())
		);
	}

	private AddPlacesToUnscheduledResult addSelectedPlacesToItinerary(
		VoteSessionRecord session,
		List<VoteCandidateRecord> candidates,
		List<VoteCandidateTally> selected,
		Instant now
	) {
		if (selected.isEmpty()) {
			return null;
		}
		Map<UUID, VoteCandidateRecord> byId = candidates.stream()
			.collect(Collectors.toMap(VoteCandidateRecord::id, candidate -> candidate, (a, b) -> a,
				LinkedHashMap::new));

		List<UnscheduledPlaceToAdd> places = new ArrayList<>();
		List<VoteCandidateRecord> orderedSelection = new ArrayList<>();
		for (VoteCandidateTally tally : selected) {
			VoteCandidateRecord candidate = byId.get(tally.candidateId());
			if (candidate == null) {
				continue;
			}
			orderedSelection.add(candidate);
			places.add(new UnscheduledPlaceToAdd(
				candidate.placeProvider(),
				candidate.externalPlaceId(),
				candidate.placeName(),
				candidate.address(),
				candidate.lat(),
				candidate.lng(),
				candidate.thumbnailUrl() == null ? null : URI.create(candidate.thumbnailUrl())
			));
		}

		AddPlacesToUnscheduledResult result = addPlacesToUnscheduledHandler.handle(
			new AddPlacesToUnscheduledCommand(
				session.tripId(),
				session.createdByUserId(),
				places,
				"vote-session:" + session.id()
			)
		);
		recordItineraryLinks(session, orderedSelection, result, now);
		return result;
	}

	private void recordItineraryLinks(
		VoteSessionRecord session,
		List<VoteCandidateRecord> orderedSelection,
		AddPlacesToUnscheduledResult result,
		Instant now
	) {
		Map<String, UUID> addedItemIdByPlace = new LinkedHashMap<>();
		for (AddedUnscheduledPlace added : result.added()) {
			addedItemIdByPlace.put(placeKey(added.placeProvider(), added.externalPlaceId()), added.itineraryItemId());
		}
		Map<String, UUID> skippedItemIdByPlace = new LinkedHashMap<>();
		for (SkippedUnscheduledPlace skipped : result.skippedDuplicates()) {
			skippedItemIdByPlace.put(
				placeKey(skipped.placeProvider(), skipped.externalPlaceId()),
				skipped.existingItineraryItemId()
			);
		}

		for (VoteCandidateRecord candidate : orderedSelection) {
			String key = placeKey(candidate.placeProvider(), candidate.externalPlaceId());
			UUID addedItemId = addedItemIdByPlace.get(key);
			if (addedItemId != null) {
				repository.insertItineraryLinkIfAbsent(new VoteItineraryLinkRecord(
					session.id(), candidate.id(), addedItemId, OUTCOME_ADDED
				), now);
				continue;
			}
			repository.insertItineraryLinkIfAbsent(new VoteItineraryLinkRecord(
				session.id(), candidate.id(), skippedItemIdByPlace.get(key), OUTCOME_SKIPPED_DUPLICATE
			), now);
		}
	}

	private void applyPreferences(VoteSessionRecord session, List<VoteStickerRecord> stickers) {
		if (stickers.isEmpty()) {
			return;
		}
		Map<UUID, VoteCandidateRecord> candidateById = repository.findCandidates(session.id()).stream()
			.collect(Collectors.toMap(VoteCandidateRecord::id, candidate -> candidate, (a, b) -> a));
		Map<UUID, VoteParticipantRecord> participantById = repository.findParticipants(session.id()).stream()
			.collect(Collectors.toMap(VoteParticipantRecord::id, participant -> participant, (a, b) -> a));

		Map<UUID, List<TripVoteStickerPlace>> placesByUser = new LinkedHashMap<>();
		for (VoteStickerRecord sticker : stickers) {
			VoteParticipantRecord participant = participantById.get(sticker.participantId());
			VoteCandidateRecord candidate = candidateById.get(sticker.candidateId());
			if (participant == null || candidate == null) {
				continue;
			}
			placesByUser
				.computeIfAbsent(participant.userId(), key -> new ArrayList<>())
				.add(new TripVoteStickerPlace(
					candidate.placeProvider(), candidate.externalPlaceId(), sticker.stickerCount()
				));
		}

		for (Map.Entry<UUID, List<TripVoteStickerPlace>> entry : placesByUser.entrySet()) {
			applyTripVotePreferenceCommandHandler.handle(new ApplyTripVotePreferenceCommand(
				session.id(), entry.getKey(), entry.getValue()
			));
		}
	}

	private CompletionOutcome existingOutcome(VoteSessionRecord session) {
		List<VoteCandidateRecord> candidates = repository.findCandidates(session.id());
		Map<UUID, Integer> stickerCounts = candidates.stream()
			.collect(Collectors.toMap(VoteCandidateRecord::id, VoteCandidateRecord::stickerCount, (a, b) -> a,
				LinkedHashMap::new));
		List<VoteCandidateTally> selected = candidates.stream()
			.filter(VoteCandidateRecord::selected)
			.map(candidate -> new VoteCandidateTally(
				candidate.id(), candidate.sortOrder(), candidate.stickerCount(), true, candidate.selectedRank()
			))
			.toList();
		return new CompletionOutcome(stickerCounts, selected, null, null, repository.findItineraryLinks(session.id()));
	}

	private String placeKey(String provider, String externalPlaceId) {
		return provider + " " + externalPlaceId;
	}

	/**
	 * 결과 확정 요약.
	 *
	 * @param stickerCountByCandidate 후보별 스티커 총합
	 * @param selected 선정된 후보와 순위
	 * @param unscheduledDayId 일정이 추가된 일차 미정 그룹. 추가된 항목이 없으면 null
	 * @param itineraryVersion 일정 반영 후 협업 version. 추가된 항목이 없으면 null
	 * @param itineraryLinks 후보별 일정 반영 결과
	 */
	public record CompletionOutcome(
		Map<UUID, Integer> stickerCountByCandidate,
		List<VoteCandidateTally> selected,
		UUID unscheduledDayId,
		Long itineraryVersion,
		List<VoteItineraryLinkRecord> itineraryLinks
	) {
	}
}
