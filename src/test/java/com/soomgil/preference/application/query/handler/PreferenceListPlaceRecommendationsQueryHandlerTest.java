package com.soomgil.preference.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateQuery;
import com.soomgil.place.application.query.handler.PlaceViewportCandidateQueryHandler;
import com.soomgil.preference.api.dto.RecommendationTab;
import com.soomgil.preference.application.query.dto.ListPlaceRecommendationsQuery;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceCalculator;
import com.soomgil.preference.domain.policy.RecommendationScorer;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceRecommendationMapper;
import com.soomgil.preference.infrastructure.persistence.row.RecommendationScoreSourceRow;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class PreferenceListPlaceRecommendationsQueryHandlerTest {

	private static final UUID TRIP_ID = UUID.fromString("00000000-0000-0000-0000-000000002001");
	private static final UUID REQUESTER_ID = UUID.fromString("00000000-0000-0000-0000-000000002002");
	private static final UUID MEMBER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000002003");
	private static final UUID MEMBER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000002004");
	private static final String BBOX = "129.0,35.0,130.0,36.0";

	private ListTripMembersHandler tripMembersHandler;
	private PlaceViewportCandidateQueryHandler placeCandidatesHandler;
	private PreferenceRecommendationMapper recommendationMapper;
	private PreferenceListPlaceRecommendationsQueryHandler handler;

	@BeforeEach
	void setUp() {
		@SuppressWarnings("unchecked")
		ObjectProvider<CurrentUserProvider> currentUserProvider = mock(ObjectProvider.class);
		tripMembersHandler = mock(ListTripMembersHandler.class);
		placeCandidatesHandler = mock(PlaceViewportCandidateQueryHandler.class);
		recommendationMapper = mock(PreferenceRecommendationMapper.class);

		when(currentUserProvider.getIfAvailable())
			.thenReturn(() -> new CurrentUser(REQUESTER_ID, "min@example.com"));
		when(tripMembersHandler.handle(any())).thenReturn(List.of(
			member(MEMBER_A_ID),
			member(MEMBER_B_ID)
		));
		when(placeCandidatesHandler.handle(any())).thenReturn(List.of(
			place("cafe", "Quiet Cafe"),
			place("beach", "Blue Beach")
		));

		handler = new PreferenceListPlaceRecommendationsQueryHandler(
			currentUserProvider,
			tripMembersHandler,
			placeCandidatesHandler,
			recommendationMapper,
			new PlaceTagEvidenceCalculator(),
			new RecommendationScorer(new BigDecimal("0.15"))
		);
	}

	@Test
	void basicTabRanksPlacesByAllActiveMembersTagPreferenceScores() {
		when(recommendationMapper.findScoreSources(any(), any())).thenReturn(List.of(
			source("cafe", "quiet", "1.00", "0.75", MEMBER_A_ID, "0.90", null),
			source("cafe", "scenic", "1.00", "0.25", MEMBER_A_ID, "0.50", null),
			source("cafe", "quiet", "1.00", "0.75", MEMBER_B_ID, "0.70", null),
			source("cafe", "scenic", "1.00", "0.25", MEMBER_B_ID, "0.60", null),
			source("beach", "quiet", "1.00", "0.25", MEMBER_A_ID, "0.90", null),
			source("beach", "scenic", "1.00", "0.75", MEMBER_A_ID, "0.50", null),
			source("beach", "quiet", "1.00", "0.25", MEMBER_B_ID, "0.70", null),
			source("beach", "scenic", "1.00", "0.75", MEMBER_B_ID, "0.60", null)
		));

		var result = handler.handle(query(RecommendationTab.BASIC));

		assertThat(result.items()).extracting(item -> item.place().externalPlaceId())
			.containsExactly("cafe", "beach");
		assertThat(result.items()).extracting(item -> item.rank())
			.containsExactly(1, 2);
		assertThat(result.items().getFirst().matchedMembers())
			.extracting(member -> member.id())
			.containsExactly(MEMBER_A_ID, MEMBER_B_ID);
		assertThat(result.items().get(1).matchedMembers()).isEmpty();
		assertThat(result.page().totalElements()).isEqualTo(2);

		ArgumentCaptor<ListTripMembersQuery> memberQuery = ArgumentCaptor.forClass(ListTripMembersQuery.class);
		verify(tripMembersHandler).handle(memberQuery.capture());
		assertThat(memberQuery.getValue().tripId()).isEqualTo(TRIP_ID);
		assertThat(memberQuery.getValue().userId()).isEqualTo(REQUESTER_ID);
		assertThat(memberQuery.getValue().status()).isEqualTo(TripMemberStatus.ACTIVE);

		ArgumentCaptor<PlaceViewportCandidateQuery> placeQuery =
			ArgumentCaptor.forClass(PlaceViewportCandidateQuery.class);
		verify(placeCandidatesHandler).handle(placeQuery.capture());
		assertThat(placeQuery.getValue().bbox()).isEqualTo(BBOX);
	}

	@Test
	void superLikeTabShowsOnlySuperLikedPlacesAndRanksByMemberCount() {
		when(placeCandidatesHandler.handle(any())).thenReturn(List.of(
			place("cafe", "Quiet Cafe"),
			place("beach", "Blue Beach"),
			place("park", "Green Park")
		));
		when(recommendationMapper.findScoreSources(any(), any())).thenReturn(List.of(
			source("cafe", "quiet", "1.00", "1.00", MEMBER_A_ID, "0.90", "SUPER_LIKE"),
			source("cafe", "quiet", "1.00", "1.00", MEMBER_B_ID, "0.70", null),
			source("beach", "scenic", "1.00", "1.00", MEMBER_A_ID, "0.50", "SUPER_LIKE"),
			source("beach", "scenic", "1.00", "1.00", MEMBER_B_ID, "0.60", "SUPER_LIKE"),
			source("park", "nature", "1.00", "1.00", MEMBER_A_ID, "0.80", null),
			source("park", "nature", "1.00", "1.00", MEMBER_B_ID, "0.80", null)
		));

		var result = handler.handle(query(RecommendationTab.SUPER_LIKE));

		assertThat(result.items()).extracting(item -> item.place().externalPlaceId())
			.containsExactly("beach", "cafe");
		assertThat(result.items().getFirst().matchedMembers())
			.extracting(member -> member.id())
			.containsExactly(MEMBER_A_ID, MEMBER_B_ID);
		assertThat(result.items().get(1).matchedMembers())
			.extracting(member -> member.id())
			.containsExactly(MEMBER_A_ID);
	}

	@Test
	void superLikeTabUsesLatestReactionAfterCountAndScoreTie() {
		when(recommendationMapper.findScoreSources(any(), any())).thenReturn(List.of(
			sourceWithTime("cafe", MEMBER_A_ID, "2026-06-19T00:00:00Z"),
			sourceWithTime("cafe", MEMBER_B_ID, null),
			sourceWithTime("beach", MEMBER_A_ID, null),
			sourceWithTime("beach", MEMBER_B_ID, "2026-06-20T00:00:00Z")
		));

		var result = handler.handle(query(RecommendationTab.SUPER_LIKE));

		assertThat(result.items()).extracting(item -> item.place().externalPlaceId())
			.containsExactly("beach", "cafe");
	}

	private ListPlaceRecommendationsQuery query(RecommendationTab tab) {
		return new ListPlaceRecommendationsQuery(TRIP_ID, BBOX, 35.5, 129.5, tab, 0, 20);
	}

	private TripMemberView member(UUID userId) {
		return new TripMemberView(
			UUID.randomUUID(),
			TRIP_ID,
			userId,
			TripMemberRole.MEMBER,
			TripAccessRole.MEMBER,
			TripMemberStatus.ACTIVE,
			Instant.parse("2026-06-20T00:00:00Z")
		);
	}

	private PlaceViewportCandidate place(String externalPlaceId, String name) {
		return new PlaceViewportCandidate(
			PlaceProvider.KTO,
			externalPlaceId,
			name,
			"Busan",
			35.5,
			129.5,
			null,
			"ATTRACTION",
			PlaceSourceStatus.AVAILABLE
		);
	}

	private RecommendationScoreSourceRow source(
		String externalPlaceId,
		String tagId,
		String confidence,
		String weight,
		UUID userId,
		String preferenceScore,
		String reaction
	) {
		return new RecommendationScoreSourceRow(
			PlaceProvider.KTO.name(),
			externalPlaceId,
			tagId,
			new BigDecimal(confidence),
			new BigDecimal(weight),
			userId.toString(),
			new BigDecimal(preferenceScore),
			reaction
		);
	}

	private RecommendationScoreSourceRow sourceWithTime(
		String externalPlaceId,
		UUID userId,
		String superLikedAt
	) {
		return new RecommendationScoreSourceRow(
			PlaceProvider.KTO.name(),
			externalPlaceId,
			"quiet",
			BigDecimal.ONE,
			BigDecimal.ONE,
			userId.toString(),
			new BigDecimal("0.70"),
			superLikedAt == null ? null : "SUPER_LIKE",
			superLikedAt == null ? null : OffsetDateTime.parse(superLikedAt)
		);
	}
}
