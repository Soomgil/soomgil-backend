package com.soomgil.preference.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceRegionCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.handler.PlaceRegionCandidateQueryHandler;
import com.soomgil.preference.application.query.dto.ListTripVoteCandidatesQuery;
import com.soomgil.preference.application.query.dto.TripVoteCandidateView;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceRecommendationMapper;
import com.soomgil.preference.infrastructure.persistence.row.RecommendationScoreSourceRow;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.ListTripRegionCodesQuery;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.application.query.handler.ListTripRegionCodesHandler;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PreferenceListTripVoteCandidatesQueryHandlerTest {

	private final ListTripMembersHandler membersHandler = mock(ListTripMembersHandler.class);
	private final ListTripRegionCodesHandler regionCodesHandler = mock(ListTripRegionCodesHandler.class);
	private final PlaceRegionCandidateQueryHandler placeCandidatesHandler =
		mock(PlaceRegionCandidateQueryHandler.class);
	private final PreferenceRecommendationMapper recommendationMapper = mock(PreferenceRecommendationMapper.class);

	private final PreferenceListTripVoteCandidatesQueryHandler handler =
		new PreferenceListTripVoteCandidatesQueryHandler(
			membersHandler, regionCodesHandler, placeCandidatesHandler, recommendationMapper
		);

	private final UUID tripId = UUID.randomUUID();
	private final UUID requesterId = UUID.randomUUID();
	private final UUID memberA = UUID.randomUUID();
	private final UUID memberB = UUID.randomUUID();

	@BeforeEach
	void stubDefaults() {
		when(membersHandler.handle(any(ListTripMembersQuery.class)))
			.thenReturn(List.of(member(memberA), member(memberB)));
		when(regionCodesHandler.handle(any(ListTripRegionCodesQuery.class)))
			.thenReturn(List.of("5011000000"));
		when(recommendationMapper.findScoreSources(anyList(), anyList())).thenReturn(List.of());
	}

	@Test
	@DisplayName("여행 지역과 활성 참여자 전원을 기준으로 후보를 만든다")
	void buildsCandidatesFromTripRegionsAndActiveMembers() {
		when(placeCandidatesHandler.handle(any(PlaceRegionCandidateQuery.class)))
			.thenReturn(List.of(place("1", "성산일출봉"), place("2", "우도")));

		List<TripVoteCandidateView> result = handler.handle(
			new ListTripVoteCandidatesQuery(tripId, requesterId, 10)
		);

		assertThat(result).hasSize(2);
		ArgumentCaptor<ListTripMembersQuery> memberCaptor = ArgumentCaptor.forClass(ListTripMembersQuery.class);
		verify(membersHandler).handle(memberCaptor.capture());
		assertThat(memberCaptor.getValue().status()).isEqualTo(TripMemberStatus.ACTIVE);

		ArgumentCaptor<PlaceRegionCandidateQuery> placeCaptor =
			ArgumentCaptor.forClass(PlaceRegionCandidateQuery.class);
		verify(placeCandidatesHandler).handle(placeCaptor.capture());
		assertThat(placeCaptor.getValue().legalRegionCodes()).containsExactly("5011000000");
	}

	@Test
	@DisplayName("기본 후보 수는 요청한 개수(기본 10개)로 잘라서 반환한다")
	void limitsCandidatesToRequestedCount() {
		when(placeCandidatesHandler.handle(any(PlaceRegionCandidateQuery.class)))
			.thenReturn(List.of(
				place("1", "a"), place("2", "b"), place("3", "c"), place("4", "d"), place("5", "e"),
				place("6", "f"), place("7", "g"), place("8", "h"), place("9", "i"), place("10", "j"),
				place("11", "k"), place("12", "l")
			));

		List<TripVoteCandidateView> result = handler.handle(
			new ListTripVoteCandidatesQuery(tripId, requesterId, 10)
		);

		assertThat(result).hasSize(10);
		assertThat(result.get(0).rank()).isEqualTo(1);
		assertThat(result.get(9).rank()).isEqualTo(10);
	}

	@Test
	@DisplayName("후보 응답에는 다른 사용자의 취향 점수나 태그가 포함되지 않는다")
	void doesNotExposeOtherMembersPreferenceSignals() {
		when(placeCandidatesHandler.handle(any(PlaceRegionCandidateQuery.class)))
			.thenReturn(List.of(place("1", "성산일출봉")));
		when(recommendationMapper.findScoreSources(anyList(), anyList())).thenReturn(List.of(
			scoreRow(memberA, "1", "tag-1", "0.92"),
			scoreRow(memberB, "1", "tag-2", "0.10")
		));

		List<TripVoteCandidateView> result = handler.handle(
			new ListTripVoteCandidatesQuery(tripId, requesterId, 10)
		);

		assertThat(result).hasSize(1);
		String rendered = result.get(0).toString();
		assertThat(rendered).doesNotContain("0.92").doesNotContain("tag-1").doesNotContain(memberA.toString());
		assertThat(TripVoteCandidateView.class.getRecordComponents())
			.extracting(java.lang.reflect.RecordComponent::getName)
			.containsExactlyInAnyOrder(
				"rank", "provider", "externalPlaceId", "name", "address", "lat", "lng", "thumbnailUrl", "category"
			);
	}

	@Test
	@DisplayName("참여자 취향 합의 점수가 높은 후보가 먼저 온다")
	void ordersByGroupConsensusScore() {
		when(placeCandidatesHandler.handle(any(PlaceRegionCandidateQuery.class)))
			.thenReturn(List.of(place("low", "낮은 점수"), place("high", "높은 점수")));
		when(recommendationMapper.findScoreSources(anyList(), anyList())).thenReturn(List.of(
			scoreRow(memberA, "low", "tag-1", "0.10"),
			scoreRow(memberB, "low", "tag-1", "0.10"),
			scoreRow(memberA, "high", "tag-1", "0.90"),
			scoreRow(memberB, "high", "tag-1", "0.90")
		));

		List<TripVoteCandidateView> result = handler.handle(
			new ListTripVoteCandidatesQuery(tripId, requesterId, 10)
		);

		assertThat(result.get(0).externalPlaceId()).isEqualTo("high");
		assertThat(result.get(1).externalPlaceId()).isEqualTo("low");
	}

	@Test
	@DisplayName("등록된 지역이 없으면 대표 목적지 키워드로 대체 검색한다")
	void fallsBackToDisplayDestinationKeyword() {
		when(regionCodesHandler.handle(any(ListTripRegionCodesQuery.class))).thenReturn(List.of());
		when(placeCandidatesHandler.handle(any(PlaceRegionCandidateQuery.class)))
			.thenReturn(List.of(place("1", "성산일출봉")));

		handler.handle(new ListTripVoteCandidatesQuery(tripId, requesterId, 10, "제주"));

		ArgumentCaptor<PlaceRegionCandidateQuery> captor = ArgumentCaptor.forClass(PlaceRegionCandidateQuery.class);
		verify(placeCandidatesHandler).handle(captor.capture());
		assertThat(captor.getValue().legalRegionCodes()).isEmpty();
		assertThat(captor.getValue().keyword()).isEqualTo("제주");
	}

	@Test
	@DisplayName("후보가 하나도 없으면 빈 목록을 반환한다")
	void returnsEmptyWhenNoCandidateExists() {
		when(placeCandidatesHandler.handle(any(PlaceRegionCandidateQuery.class))).thenReturn(List.of());

		List<TripVoteCandidateView> result = handler.handle(
			new ListTripVoteCandidatesQuery(tripId, requesterId, 10)
		);

		assertThat(result).isEmpty();
		verify(recommendationMapper, never()).findScoreSources(anyList(), anyList());
	}

	@Test
	@DisplayName("활성 참여자가 없으면 후보를 만들지 않는다")
	void returnsEmptyWhenNoActiveMember() {
		when(membersHandler.handle(any(ListTripMembersQuery.class))).thenReturn(List.of());

		List<TripVoteCandidateView> result = handler.handle(
			new ListTripVoteCandidatesQuery(tripId, requesterId, 10)
		);

		assertThat(result).isEmpty();
		verify(placeCandidatesHandler, never()).handle(any(PlaceRegionCandidateQuery.class));
	}

	private TripMemberView member(UUID userId) {
		return new TripMemberView(
			UUID.randomUUID(), tripId, userId, TripMemberRole.MEMBER, TripAccessRole.MEMBER,
			TripMemberStatus.ACTIVE, Instant.now()
		);
	}

	private PlaceViewportCandidate place(String externalPlaceId, String name) {
		return new PlaceViewportCandidate(
			PlaceProvider.KTO, externalPlaceId, name, "제주특별자치도",
			33.45, 126.94, null, "A01", PlaceSourceStatus.AVAILABLE
		);
	}

	private RecommendationScoreSourceRow scoreRow(UUID userId, String externalPlaceId, String tagId, String score) {
		return new RecommendationScoreSourceRow(
			PlaceProvider.KTO.name(), externalPlaceId, tagId,
			new BigDecimal("0.9"), new BigDecimal("1.0"),
			userId.toString(), new BigDecimal(score), null, OffsetDateTime.now(), null, null
		);
	}
}
