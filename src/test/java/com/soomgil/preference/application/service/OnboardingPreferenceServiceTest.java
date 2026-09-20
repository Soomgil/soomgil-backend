package com.soomgil.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.api.dto.CompleteOnboardingPreferenceSurveyRequest;
import com.soomgil.preference.api.dto.OnboardingPreferenceAnswer;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import com.soomgil.preference.application.command.handler.UpsertSwipeReactionCommandHandler;
import com.soomgil.preference.domain.policy.PreferenceSource;
import com.soomgil.preference.infrastructure.persistence.mapper.OnboardingPreferenceMapper;
import com.soomgil.preference.infrastructure.persistence.row.OnboardingSurveyPlaceRefRow;
import com.soomgil.preference.infrastructure.persistence.row.OnboardingSurveyVersionRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OnboardingPreferenceServiceTest {

	private static final UUID USER_ID = UUID.fromString("a7397fef-706c-4b49-9be0-35d63c14c595");
	private static final UUID SURVEY_ID = UUID.fromString("8f4a2b66-e120-45d2-9bc7-5b7708de1001");

	private final OnboardingPreferenceMapper mapper = mock(OnboardingPreferenceMapper.class);
	private final UpsertSwipeReactionCommandHandler reactionHandler =
		mock(UpsertSwipeReactionCommandHandler.class);
	private final TourismPlaceFeedClient tourismPlaceFeedClient = mock(TourismPlaceFeedClient.class);
	private final SwipeTagPreparationService tagPreparationService = mock(SwipeTagPreparationService.class);
	private final OnboardingPreferenceService service =
		new OnboardingPreferenceService(
			mapper,
			reactionHandler,
			tourismPlaceFeedClient,
			tagPreparationService
		);

	@Test
	void completesAllTenAnswersAsTripleWeightedOnboardingEvidence() {
		List<OnboardingSurveyPlaceRefRow> places = places();
		OffsetDateTime completedAt = OffsetDateTime.parse("2026-09-19T10:00:00+09:00");
		when(mapper.findActiveVersion()).thenReturn(Optional.of(new OnboardingSurveyVersionRow(
			SURVEY_ID,
			"jeju-diversity-v1",
			10
		)));
		when(mapper.findPlaces(SURVEY_ID)).thenReturn(places);
		when(mapper.findCompletedAt(USER_ID)).thenReturn(null, completedAt);

		var request = new CompleteOnboardingPreferenceSurveyRequest(
			SURVEY_ID,
			places.stream()
				.map(place -> new OnboardingPreferenceAnswer(
					PlaceProvider.KTO,
					place.externalPlaceId(),
					place.sortOrder() == 1
						? SwipeReaction.SUPER_LIKE
						: place.sortOrder() % 2 == 0 ? SwipeReaction.NOPE : SwipeReaction.LIKE
				))
				.toList()
		);

		var result = service.complete(USER_ID, request);

		ArgumentCaptor<UpsertSwipeReactionCommand> commandCaptor =
			ArgumentCaptor.forClass(UpsertSwipeReactionCommand.class);
		verify(reactionHandler, times(10)).handle(commandCaptor.capture());
		assertThat(commandCaptor.getAllValues())
			.allSatisfy(command -> {
				assertThat(command.source()).isEqualTo(PreferenceSource.ONBOARDING);
				assertThat(command.source().evidenceMultiplier()).isEqualByComparingTo("3.0");
				assertThat(command.sourceResourceId()).startsWith(SURVEY_ID.toString() + ":");
			});
		assertThat(commandCaptor.getAllValues().getFirst().reaction()).isEqualTo(SwipeReaction.SUPER_LIKE);
		verify(mapper, times(10)).upsertResponse(any(), any(), any(), any(), any());
		verify(mapper).markCompleted(USER_ID);
		assertThat(result.completedAt()).isEqualTo(completedAt);
	}

	@Test
	void returnsExistingCompletionWithoutApplyingEvidenceAgain() {
		OffsetDateTime completedAt = OffsetDateTime.parse("2026-09-19T10:00:00+09:00");
		List<OnboardingSurveyPlaceRefRow> places = places();
		when(mapper.findActiveVersion()).thenReturn(Optional.of(new OnboardingSurveyVersionRow(
			SURVEY_ID,
			"jeju-diversity-v1",
			10
		)));
		when(mapper.findPlaces(SURVEY_ID)).thenReturn(places);
		when(mapper.findCompletedAt(USER_ID)).thenReturn(completedAt);
		var request = new CompleteOnboardingPreferenceSurveyRequest(
			SURVEY_ID,
			places.stream()
				.map(place -> new OnboardingPreferenceAnswer(
					PlaceProvider.KTO,
					place.externalPlaceId(),
					SwipeReaction.LIKE
				))
				.toList()
		);

		var result = service.complete(USER_ID, request);

		assertThat(result.completedAt()).isEqualTo(completedAt);
		verify(reactionHandler, never()).handle(any());
		verify(mapper, never()).markCompleted(any());
	}

	@Test
	void exposesComputedCompletionStateInTheApiPayload() throws Exception {
		OffsetDateTime completedAt = OffsetDateTime.parse("2026-09-19T10:00:00+09:00");
		when(mapper.findActiveVersion()).thenReturn(Optional.of(new OnboardingSurveyVersionRow(
			SURVEY_ID,
			"jeju-diversity-v1",
			10
		)));
		when(mapper.findPlaces(SURVEY_ID)).thenReturn(places());
		when(mapper.findCompletedAt(USER_ID)).thenReturn(completedAt);
		when(tourismPlaceFeedClient.fetchFixedPlaces(any())).thenReturn(livePlaces());
		when(tagPreparationService.prepare(any())).thenReturn(Map.of());

		var response = service.getSurvey(USER_ID);
		var objectMapper = JsonMapper.builder().findAndAddModules().build();

		assertThat(objectMapper.writeValueAsString(response)).contains("\"completed\":true");
		assertThat(response.places().getFirst().name()).isEqualTo("실제 관광지 1");
	}

	private List<OnboardingSurveyPlaceRefRow> places() {
		return java.util.stream.IntStream.rangeClosed(1, 10)
			.mapToObj(index -> new OnboardingSurveyPlaceRefRow(
				"KTO",
				String.valueOf(126400 + index),
				index
			))
			.toList();
	}

	private List<TourismPlaceFeedItem> livePlaces() {
		return java.util.stream.IntStream.rangeClosed(1, 10)
			.mapToObj(index -> new TourismPlaceFeedItem(
				String.valueOf(126400 + index),
				"실제 관광지 " + index,
				"실제 주소 " + index,
				37.0,
				127.0,
				"https://tong.visitkorea.or.kr/place-" + index + ".jpg",
				"관광지",
				"실제 설명 " + index,
				List.of("https://tong.visitkorea.or.kr/place-" + index + ".jpg")
			))
			.toList();
	}
}
