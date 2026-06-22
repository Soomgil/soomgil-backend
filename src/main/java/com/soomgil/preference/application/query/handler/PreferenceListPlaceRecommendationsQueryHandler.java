package com.soomgil.preference.application.query.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateQuery;
import com.soomgil.place.application.query.handler.PlaceViewportCandidateQueryHandler;
import com.soomgil.preference.api.dto.PagedPlaceRecommendation;
import com.soomgil.preference.api.dto.PlaceRecommendation;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.config.PreferencePolicyProperties;
import com.soomgil.preference.domain.policy.PlaceTagEvidence;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceCalculator;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceInput;
import com.soomgil.preference.domain.policy.RecommendationScorer;
import com.soomgil.preference.domain.policy.RecommendationTagScoreInput;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceRecommendationMapper;
import com.soomgil.preference.infrastructure.persistence.row.RecommendationScoreSourceRow;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.user.api.dto.UserSummary;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * active 여행 멤버의 누적 태그 선호도로 viewport 장소 추천을 계산한다.
 *
 * <p>다른 멤버의 원시 선호도 점수는 응답에 포함하지 않고, 기준을 넘긴 멤버의 공개 요약만 반환한다.
 * SUPER_LIKE 탭은 최종 SUPER_LIKE 멤버 수를 우선하고 태그 추천 점수는 동점 처리에 사용한다.
 */
@Service
public class PreferenceListPlaceRecommendationsQueryHandler
	implements ListPlaceRecommendationsQueryHandler {

	private static final int MAX_CANDIDATE_COUNT = 200;
	private static final BigDecimal NEUTRAL_SCORE = new BigDecimal("0.5");
	private static final double EARTH_RADIUS_METERS = 6_371_000.0;

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final ListTripMembersHandler tripMembersHandler;
	private final PlaceViewportCandidateQueryHandler placeCandidatesHandler;
	private final PreferenceRecommendationMapper recommendationMapper;
	private final PlaceTagEvidenceCalculator evidenceCalculator;
	private final RecommendationScorer recommendationScorer;

	@Autowired
	public PreferenceListPlaceRecommendationsQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		ListTripMembersHandler tripMembersHandler,
		PlaceViewportCandidateQueryHandler placeCandidatesHandler,
		PreferenceRecommendationMapper recommendationMapper,
		PreferencePolicyProperties properties
	) {
		this(
			currentUserProvider,
			tripMembersHandler,
			placeCandidatesHandler,
			recommendationMapper,
			new PlaceTagEvidenceCalculator(),
			new RecommendationScorer(
				properties.getRecommendation().getMatchedMemberThreshold()
			)
		);
	}

	public PreferenceListPlaceRecommendationsQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		ListTripMembersHandler tripMembersHandler,
		PlaceViewportCandidateQueryHandler placeCandidatesHandler,
		PreferenceRecommendationMapper recommendationMapper,
		PlaceTagEvidenceCalculator evidenceCalculator,
		RecommendationScorer recommendationScorer
	) {
		this.currentUserProvider = currentUserProvider;
		this.tripMembersHandler = tripMembersHandler;
		this.placeCandidatesHandler = placeCandidatesHandler;
		this.recommendationMapper = recommendationMapper;
		this.evidenceCalculator = evidenceCalculator;
		this.recommendationScorer = recommendationScorer;
	}

	@Override
	@Transactional(readOnly = true)
	public PagedPlaceRecommendation handle(ListPlaceRecommendationsQuery query) {
		validate(query);
		RecommendationTab tab = query.tab() == null ? RecommendationTab.BASIC : query.tab();
		UUID currentUserId = currentUserId();
		List<TripMemberView> members = tripMembersHandler.handle(new ListTripMembersQuery(
			query.tripId(),
			currentUserId,
			TripMemberStatus.ACTIVE
		));
		List<PlaceViewportCandidate> candidates = placeCandidatesHandler.handle(
			new PlaceViewportCandidateQuery(query.bbox(), null, MAX_CANDIDATE_COUNT)
		);
		if (members.isEmpty() || candidates.isEmpty()) {
			return emptyPage(query);
		}

		List<RecommendationScoreSourceRow> sourceRows = recommendationMapper.findScoreSources(
			members.stream().map(member -> member.userId().toString()).toList(),
			candidates.stream()
				.map(candidate -> new PlaceRef(candidate.provider(), candidate.externalPlaceId()))
				.toList()
		);
		Map<PlaceKey, List<RecommendationScoreSourceRow>> rowsByPlace = sourceRows.stream()
			.collect(Collectors.groupingBy(
				row -> new PlaceKey(row.provider(), row.externalPlaceId()),
				LinkedHashMap::new,
				Collectors.toList()
			));
		Map<UUID, UserSummary> memberSummaries = memberSummaries(members, sourceRows);

		List<ScoredRecommendation> scored = candidates.stream()
			.map(candidate -> score(
				candidate,
				members,
				rowsByPlace.getOrDefault(PlaceKey.from(candidate), List.of()),
				memberSummaries,
				query.centerLat(),
				query.centerLng()
			))
			.filter(item -> tab != RecommendationTab.SUPER_LIKE || item.superLikeCount() > 0)
			.sorted(comparator(tab))
			.toList();

		return page(scored, query, tab);
	}

	private ScoredRecommendation score(
		PlaceViewportCandidate candidate,
		List<TripMemberView> members,
		List<RecommendationScoreSourceRow> rows,
		Map<UUID, UserSummary> memberSummaries,
		Double centerLat,
		Double centerLng
	) {
		List<PlaceTagEvidence> evidence = evidence(rows);
		Map<UUID, BigDecimal> memberScores = new LinkedHashMap<>();
		for (TripMemberView member : members) {
			memberScores.put(member.userId(), memberScore(member.userId(), rows, evidence));
		}

		Set<UUID> superLikedMemberIds = rows.stream()
			.filter(row -> "SUPER_LIKE".equals(row.reaction()))
			.map(row -> UUID.fromString(row.userId()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
		OffsetDateTime latestSuperLikedAt = rows.stream()
			.filter(row -> "SUPER_LIKE".equals(row.reaction()))
			.map(RecommendationScoreSourceRow::lastReactedAt)
			.filter(Objects::nonNull)
			.max(Comparator.naturalOrder())
			.orElse(null);
		List<UserSummary> matchedMembers = memberScores.entrySet().stream()
			.filter(entry -> recommendationScorer.isMatchedMember(entry.getValue()))
			.map(Map.Entry::getKey)
			.map(memberSummaries::get)
			.filter(Objects::nonNull)
			.toList();
		List<UserSummary> superLikedMembers = superLikedMemberIds.stream()
			.map(memberSummaries::get)
			.filter(Objects::nonNull)
			.toList();
		BigDecimal superLikeTagMatchScore = recommendationScorer.calculateGroupScore(
			superLikedMemberIds.stream()
				.map(memberScores::get)
				.filter(Objects::nonNull)
				.toList()
		);

		return new ScoredRecommendation(
			candidate,
			recommendationScorer.calculateGroupScore(new ArrayList<>(memberScores.values())),
			superLikedMemberIds.size(),
			superLikeTagMatchScore,
			matchedMembers,
			superLikedMembers,
			latestSuperLikedAt,
			distanceMeters(centerLat, centerLng, candidate.lat(), candidate.lng())
		);
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
		UUID memberId,
		List<RecommendationScoreSourceRow> rows,
		List<PlaceTagEvidence> evidence
	) {
		if (evidence.isEmpty()) {
			return NEUTRAL_SCORE;
		}
		Map<String, BigDecimal> preferences = rows.stream()
			.filter(row -> memberId.toString().equals(row.userId()) && row.tagId() != null)
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

	private Map<UUID, UserSummary> memberSummaries(
		List<TripMemberView> members,
		List<RecommendationScoreSourceRow> rows
	) {
		Map<UUID, RecommendationScoreSourceRow> profileRows = rows.stream()
			.collect(Collectors.toMap(
				row -> UUID.fromString(row.userId()),
				Function.identity(),
				(first, second) -> first
			));
		Map<UUID, UserSummary> result = new LinkedHashMap<>();
		for (TripMemberView member : members) {
			RecommendationScoreSourceRow profile = profileRows.get(member.userId());
			String displayName = profile == null || profile.displayName() == null || profile.displayName().isBlank()
				? member.userId().toString()
				: profile.displayName();
			result.put(member.userId(), new UserSummary(
				member.userId(),
				displayName,
				profile == null ? null : toUri(profile.profileImageUrl())
			));
		}
		return result;
	}

	private PagedPlaceRecommendation page(
		List<ScoredRecommendation> scored,
		ListPlaceRecommendationsQuery query,
		RecommendationTab tab
	) {
		long offset = (long) query.page() * query.size();
		int fromIndex = (int) Math.min(offset, scored.size());
		int toIndex = Math.min(fromIndex + query.size(), scored.size());
		List<PlaceRecommendation> items = new ArrayList<>();
		for (int index = fromIndex; index < toIndex; index++) {
			ScoredRecommendation item = scored.get(index);
			List<UserSummary> matched = tab == RecommendationTab.SUPER_LIKE
				? item.superLikedMembers()
				: item.matchedMembers();
			items.add(new PlaceRecommendation(
				toSummary(item.place()),
				matched,
				index + 1,
				item.distanceMeters(),
				recommendationReason(tab, matched.size())
			));
		}

		long totalElements = scored.size();
		int totalPages = totalElements == 0
			? 0
			: (int) Math.ceil((double) totalElements / query.size());
		return new PagedPlaceRecommendation(
			List.copyOf(items),
			new PageMeta(query.page(), query.size(), totalElements, totalPages, List.of())
		);
	}

	private Comparator<ScoredRecommendation> comparator(RecommendationTab tab) {
		Comparator<ScoredRecommendation> tieBreakers = Comparator
			.comparing(ScoredRecommendation::distanceMeters, Comparator.nullsLast(Double::compareTo))
			.thenComparing(item -> item.place().provider().name())
			.thenComparing(item -> item.place().externalPlaceId());
		if (tab == RecommendationTab.SUPER_LIKE) {
			return Comparator.comparingInt(ScoredRecommendation::superLikeCount)
				.reversed()
				.thenComparing(
					ScoredRecommendation::superLikeTagMatchScore,
					Comparator.reverseOrder()
				)
				.thenComparing(
					ScoredRecommendation::latestSuperLikedAt,
					Comparator.nullsLast(Comparator.reverseOrder())
				)
				.thenComparing(tieBreakers);
		}
		return Comparator.comparing(ScoredRecommendation::groupScore, Comparator.reverseOrder())
			.thenComparing(tieBreakers);
	}

	private String recommendationReason(RecommendationTab tab, int matchedMemberCount) {
		if (tab == RecommendationTab.SUPER_LIKE) {
			return matchedMemberCount + "명이 꼭 가고 싶어 해요";
		}
		if (matchedMemberCount > 0) {
			return matchedMemberCount + "명의 취향과 잘 맞아요";
		}
		return "여행 멤버의 취향을 반영했어요";
	}

	private Double distanceMeters(Double centerLat, Double centerLng, Double lat, Double lng) {
		if (centerLat == null || centerLng == null || lat == null || lng == null) {
			return null;
		}
		double latDistance = Math.toRadians(lat - centerLat);
		double lngDistance = Math.toRadians(lng - centerLng);
		double startLat = Math.toRadians(centerLat);
		double endLat = Math.toRadians(lat);
		double haversine = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
			+ Math.cos(startLat) * Math.cos(endLat)
			* Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
		return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
	}

	private PlaceSummary toSummary(PlaceViewportCandidate place) {
		return new PlaceSummary(
			place.provider(),
			place.externalPlaceId(),
			place.name(),
			place.address(),
			place.lat(),
			place.lng(),
			place.thumbnailUrl(),
			place.category(),
			place.sourceStatus()
		);
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to list place recommendations.");
		}
		return provider.currentUserId();
	}

	private void validate(ListPlaceRecommendationsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		Objects.requireNonNull(query.tripId(), "tripId must not be null");
		if (query.bbox() == null || query.bbox().isBlank()) {
			throw new IllegalArgumentException("bbox must not be blank");
		}
		if (query.page() < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (query.size() < 1 || query.size() > 100) {
			throw new IllegalArgumentException("size must be between 1 and 100");
		}
	}

	private PagedPlaceRecommendation emptyPage(ListPlaceRecommendationsQuery query) {
		return new PagedPlaceRecommendation(
			List.of(),
			new PageMeta(query.page(), query.size(), 0L, 0, List.of())
		);
	}

	private record PlaceKey(String provider, String externalPlaceId) {

		private static PlaceKey from(PlaceViewportCandidate place) {
			return new PlaceKey(place.provider().name(), place.externalPlaceId());
		}
	}

	private record ScoredRecommendation(
		PlaceViewportCandidate place,
		BigDecimal groupScore,
		int superLikeCount,
		BigDecimal superLikeTagMatchScore,
		List<UserSummary> matchedMembers,
		List<UserSummary> superLikedMembers,
		OffsetDateTime latestSuperLikedAt,
		Double distanceMeters
	) {
	}
}
