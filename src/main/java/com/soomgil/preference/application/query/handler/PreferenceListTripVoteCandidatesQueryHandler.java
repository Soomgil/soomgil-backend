package com.soomgil.preference.application.query.handler;

import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.application.query.dto.PlaceRegionCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.handler.PlaceRegionCandidateQueryHandler;
import com.soomgil.preference.application.query.dto.ListTripVoteCandidatesQuery;
import com.soomgil.preference.application.query.dto.TripVoteCandidateView;
import com.soomgil.preference.domain.policy.PlaceTagEvidence;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceCalculator;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceInput;
import com.soomgil.preference.domain.policy.RecommendationScorer;
import com.soomgil.preference.domain.policy.RecommendationTagScoreInput;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceRecommendationMapper;
import com.soomgil.preference.infrastructure.persistence.row.RecommendationScoreSourceRow;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.ListTripRegionCodesQuery;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.application.query.handler.ListTripRegionCodesHandler;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여행 지역과 활성 참여자 전원의 누적 취향으로 투표 후보를 구성한다.
 *
 * <p>후보 풀은 지도 viewport가 아니라 여행방에 등록된 법정동 코드에서 가져온다. 등록된 지역이 없으면
 * 여행방 대표 목적지 문자열을 검색어로 사용한다.
 *
 * <p>정렬은 지도 추천 패널과 같은 합의 점수({@code calculateGroupConsensusScore})를 사용해
 * 한 참여자의 강한 선호가 다른 참여자의 낮은 선호를 덮지 않게 한다.
 *
 * <p>응답에는 다른 참여자의 취향 점수, 세부 태그, matched member 정보를 포함하지 않는다.
 */
@Service
public class PreferenceListTripVoteCandidatesQueryHandler implements ListTripVoteCandidatesQueryHandler {

	private static final int MAX_CANDIDATE_POOL = 200;
	private static final int DEFAULT_LIMIT = 10;
	private static final BigDecimal NEUTRAL_SCORE = new BigDecimal("0.5");

	private final ListTripMembersHandler membersHandler;
	private final ListTripRegionCodesHandler regionCodesHandler;
	private final PlaceRegionCandidateQueryHandler placeCandidatesHandler;
	private final PreferenceRecommendationMapper recommendationMapper;
	private final PlaceTagEvidenceCalculator evidenceCalculator;
	private final RecommendationScorer recommendationScorer;

	public PreferenceListTripVoteCandidatesQueryHandler(
		ListTripMembersHandler membersHandler,
		ListTripRegionCodesHandler regionCodesHandler,
		PlaceRegionCandidateQueryHandler placeCandidatesHandler,
		PreferenceRecommendationMapper recommendationMapper
	) {
		this.membersHandler = Objects.requireNonNull(membersHandler, "membersHandler must not be null");
		this.regionCodesHandler = Objects.requireNonNull(regionCodesHandler, "regionCodesHandler must not be null");
		this.placeCandidatesHandler =
			Objects.requireNonNull(placeCandidatesHandler, "placeCandidatesHandler must not be null");
		this.recommendationMapper =
			Objects.requireNonNull(recommendationMapper, "recommendationMapper must not be null");
		this.evidenceCalculator = new PlaceTagEvidenceCalculator();
		this.recommendationScorer = new RecommendationScorer(new BigDecimal("0.15"));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TripVoteCandidateView> handle(ListTripVoteCandidatesQuery query) {
		int limit = query.limit() < 1 ? DEFAULT_LIMIT : query.limit();

		List<TripMemberView> members = membersHandler.handle(new ListTripMembersQuery(
			query.tripId(), query.requesterUserId(), TripMemberStatus.ACTIVE
		));
		if (members.isEmpty()) {
			return List.of();
		}

		// 방장이 이번 투표용 지역을 골랐으면 그 지역을, 아니면 여행방에 등록된 지역을 쓴다.
		List<String> regionCodes = !query.regionCodes().isEmpty()
			? query.regionCodes()
			: regionCodesHandler.handle(new ListTripRegionCodesQuery(query.tripId(), query.requesterUserId()));
		List<PlaceViewportCandidate> candidates = placeCandidatesHandler.handle(new PlaceRegionCandidateQuery(
			regionCodes, query.destinationKeyword(), null, MAX_CANDIDATE_POOL
		));
		if (candidates.isEmpty()) {
			return List.of();
		}

		Map<PlaceKey, List<RecommendationScoreSourceRow>> rowsByPlace = loadScoreSources(members, candidates);

		List<ScoredCandidate> scored = candidates.stream()
			.map(candidate -> new ScoredCandidate(
				candidate,
				groupScore(candidate, members, rowsByPlace.getOrDefault(PlaceKey.from(candidate), List.of()))
			))
			.sorted(Comparator
				.comparing(ScoredCandidate::groupScore, Comparator.reverseOrder())
				.thenComparing(item -> item.place().externalPlaceId()))
			.limit(limit)
			.toList();

		List<TripVoteCandidateView> result = new ArrayList<>();
		for (int index = 0; index < scored.size(); index++) {
			PlaceViewportCandidate place = scored.get(index).place();
			result.add(new TripVoteCandidateView(
				index + 1,
				place.provider(),
				place.externalPlaceId(),
				place.name(),
				place.address(),
				place.lat(),
				place.lng(),
				place.thumbnailUrl(),
				place.category()
			));
		}
		return List.copyOf(result);
	}

	private Map<PlaceKey, List<RecommendationScoreSourceRow>> loadScoreSources(
		List<TripMemberView> members,
		List<PlaceViewportCandidate> candidates
	) {
		return recommendationMapper.findScoreSources(
				members.stream().map(member -> member.userId().toString()).toList(),
				candidates.stream()
					.map(candidate -> new PlaceRef(candidate.provider(), candidate.externalPlaceId()))
					.toList()
			).stream()
			.collect(Collectors.groupingBy(
				row -> new PlaceKey(row.provider(), row.externalPlaceId()),
				LinkedHashMap::new,
				Collectors.toList()
			));
	}

	private BigDecimal groupScore(
		PlaceViewportCandidate candidate,
		List<TripMemberView> members,
		List<RecommendationScoreSourceRow> rows
	) {
		List<PlaceTagEvidence> evidence = evidence(rows);
		List<BigDecimal> memberScores = members.stream()
			.map(member -> memberScore(member, rows, evidence))
			.toList();
		return recommendationScorer.calculateGroupConsensusScore(new ArrayList<>(memberScores));
	}

	private List<PlaceTagEvidence> evidence(List<RecommendationScoreSourceRow> rows) {
		Map<String, PlaceTagEvidenceInput> uniqueTags = new LinkedHashMap<>();
		for (RecommendationScoreSourceRow row : rows) {
			if (row.tagId() != null && row.confidence() != null && row.weight() != null) {
				uniqueTags.putIfAbsent(
					row.tagId(),
					new PlaceTagEvidenceInput(row.tagId(), row.confidence(), row.weight())
				);
			}
		}
		return evidenceCalculator.calculate(new ArrayList<>(uniqueTags.values()));
	}

	private BigDecimal memberScore(
		TripMemberView member,
		List<RecommendationScoreSourceRow> rows,
		List<PlaceTagEvidence> evidence
	) {
		if (evidence.isEmpty()) {
			return NEUTRAL_SCORE;
		}
		Map<String, BigDecimal> preferences = rows.stream()
			.filter(row -> member.userId().toString().equals(row.userId()) && row.tagId() != null)
			.collect(Collectors.toMap(
				RecommendationScoreSourceRow::tagId,
				RecommendationScoreSourceRow::preferenceScore,
				(first, second) -> first
			));
		List<RecommendationTagScoreInput> inputs = evidence.stream()
			.map(tag -> new RecommendationTagScoreInput(
				preferences.getOrDefault(tag.tagId(), NEUTRAL_SCORE),
				tag.value()
			))
			.toList();
		return recommendationScorer.calculateMemberScore(inputs);
	}

	private record ScoredCandidate(PlaceViewportCandidate place, BigDecimal groupScore) {
	}

	private record PlaceKey(String provider, String externalPlaceId) {

		private static PlaceKey from(PlaceViewportCandidate place) {
			return new PlaceKey(place.provider().name(), place.externalPlaceId());
		}
	}
}
