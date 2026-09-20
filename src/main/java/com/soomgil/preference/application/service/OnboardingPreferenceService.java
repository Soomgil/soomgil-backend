package com.soomgil.preference.application.service;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.port.TourismPlaceFeedClient;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.api.dto.CompleteOnboardingPreferenceSurveyRequest;
import com.soomgil.preference.api.dto.OnboardingPreferenceAnswer;
import com.soomgil.preference.api.dto.OnboardingPreferenceCompletionResponse;
import com.soomgil.preference.api.dto.OnboardingPreferencePlace;
import com.soomgil.preference.api.dto.OnboardingPreferenceSurveyResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import com.soomgil.preference.application.command.handler.UpsertSwipeReactionCommandHandler;
import com.soomgil.preference.domain.policy.PreferenceSource;
import com.soomgil.preference.infrastructure.persistence.mapper.OnboardingPreferenceMapper;
import com.soomgil.preference.infrastructure.persistence.row.OnboardingSurveyPlaceRefRow;
import com.soomgil.preference.infrastructure.persistence.row.OnboardingSurveyVersionRow;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신규 사용자의 고정 관광지 10개 설문을 조회하고 완료한다.
 *
 * <p>완료 요청은 활성 version의 모든 장소가 포함된 경우에만 같은 transaction에서 응답 로그,
 * 3배 취향 근거, 사용자 완료 시각을 함께 저장한다. 동일 요청의 재시도는 완료 시각을 바꾸지 않는다.
 */
@Service
public class OnboardingPreferenceService {

	private final OnboardingPreferenceMapper mapper;
	private final UpsertSwipeReactionCommandHandler reactionHandler;
	private final TourismPlaceFeedClient tourismPlaceFeedClient;
	private final SwipeTagPreparationService tagPreparationService;

	public OnboardingPreferenceService(
		OnboardingPreferenceMapper mapper,
		UpsertSwipeReactionCommandHandler reactionHandler,
		TourismPlaceFeedClient tourismPlaceFeedClient,
		SwipeTagPreparationService tagPreparationService
	) {
		this.mapper = mapper;
		this.reactionHandler = reactionHandler;
		this.tourismPlaceFeedClient = tourismPlaceFeedClient;
		this.tagPreparationService = tagPreparationService;
	}

	/**
	 * 활성 설문 장소와 사용자의 완료 상태를 조회한다.
	 *
	 * @param userId 현재 사용자 id
	 * @return 고정 순서의 설문 응답
	 */
	@Transactional(readOnly = true)
	public OnboardingPreferenceSurveyResponse getSurvey(UUID userId) {
		OnboardingSurveyVersionRow version = activeVersion();
		List<OnboardingSurveyPlaceRefRow> rows = surveyPlaces(version);
		List<TourismPlaceFeedItem> livePlaces = tourismPlaceFeedClient.fetchFixedPlaces(
			rows.stream().map(OnboardingSurveyPlaceRefRow::externalPlaceId).toList()
		);
		Map<String, TourismPlaceFeedItem> livePlaceById = livePlaces.stream()
			.collect(Collectors.toMap(TourismPlaceFeedItem::externalPlaceId, Function.identity()));
		if (livePlaceById.size() != rows.size()) {
			throw new BusinessException(
				ErrorCode.CONFLICT,
				"가입 취향 설문의 관광공사 장소 정보를 모두 불러오지 못했습니다."
			);
		}
		Map<String, SwipeTagPreparation> tagPreparation = tagPreparationService.prepare(livePlaces);
		return new OnboardingPreferenceSurveyResponse(
			version.id(),
			version.code(),
			version.requiredPlaceCount(),
			mapper.findCompletedAt(userId),
			rows.stream()
				.map(row -> toPlace(
					row,
					livePlaceById.get(row.externalPlaceId()),
					tagPreparation.get(row.externalPlaceId())
				))
				.toList()
		);
	}

	/**
	 * 정확히 10개의 응답을 3배 취향 근거와 함께 원자적으로 저장한다.
	 *
	 * @param userId 현재 사용자 id
	 * @param request 활성 설문의 전체 응답
	 * @return 완료된 설문 version과 완료 시각
	 */
	@Transactional
	public OnboardingPreferenceCompletionResponse complete(
		UUID userId,
		CompleteOnboardingPreferenceSurveyRequest request
	) {
		OnboardingSurveyVersionRow version = activeVersion();
		if (!version.id().equals(request.surveyVersionId())) {
			throw invalid("활성화된 취향 설문 version이 아닙니다.");
		}

		List<OnboardingSurveyPlaceRefRow> places = surveyPlaces(version);
		Map<PlaceKey, OnboardingPreferenceAnswer> answers = validateAnswers(request.responses(), places);
		OffsetDateTime completedAt = mapper.findCompletedAt(userId);
		if (completedAt != null) {
			return new OnboardingPreferenceCompletionResponse(version.id(), completedAt);
		}

		for (OnboardingSurveyPlaceRefRow place : places) {
			PlaceKey key = new PlaceKey(place.provider(), place.externalPlaceId());
			OnboardingPreferenceAnswer answer = answers.get(key);
			String sourceResourceId = version.id() + ":" + place.externalPlaceId();
			reactionHandler.handle(new UpsertSwipeReactionCommand(
				answer.provider(),
				answer.externalPlaceId(),
				answer.reaction(),
				null,
				PreferenceSource.ONBOARDING,
				sourceResourceId
			));
			mapper.upsertResponse(
				userId,
				version.id(),
				answer.provider().name(),
				answer.externalPlaceId(),
				answer.reaction().name()
			);
		}

		mapper.markCompleted(userId);
		OffsetDateTime savedAt = mapper.findCompletedAt(userId);
		return new OnboardingPreferenceCompletionResponse(
			version.id(),
			savedAt != null ? savedAt : OffsetDateTime.now(ZoneOffset.UTC)
		);
	}

	private OnboardingSurveyVersionRow activeVersion() {
		return mapper.findActiveVersion()
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "활성 가입 취향 설문이 없습니다."));
	}

	private List<OnboardingSurveyPlaceRefRow> surveyPlaces(OnboardingSurveyVersionRow version) {
		List<OnboardingSurveyPlaceRefRow> places = mapper.findPlaces(version.id());
		if (places.size() != version.requiredPlaceCount()) {
			throw new BusinessException(
				ErrorCode.CONFLICT,
				"활성 가입 취향 설문은 정확히 " + version.requiredPlaceCount() + "개 장소를 가져야 합니다."
			);
		}
		return places;
	}

	private Map<PlaceKey, OnboardingPreferenceAnswer> validateAnswers(
		List<OnboardingPreferenceAnswer> responses,
		List<OnboardingSurveyPlaceRefRow> places
	) {
		Map<PlaceKey, OnboardingPreferenceAnswer> answers = new LinkedHashMap<>();
		for (OnboardingPreferenceAnswer answer : responses) {
			PlaceKey key = new PlaceKey(answer.provider().name(), answer.externalPlaceId());
			if (answers.putIfAbsent(key, answer) != null) {
				throw invalid("같은 관광지 응답을 중복 제출할 수 없습니다.");
			}
		}

		List<PlaceKey> expected = places.stream()
			.map(place -> new PlaceKey(place.provider(), place.externalPlaceId()))
			.toList();
		if (answers.size() != expected.size() || !answers.keySet().containsAll(expected)) {
			throw invalid("활성 설문의 관광지 10개를 모두 평가해야 합니다.");
		}
		return answers;
	}

	private OnboardingPreferencePlace toPlace(
		OnboardingSurveyPlaceRefRow row,
		TourismPlaceFeedItem livePlace,
		SwipeTagPreparation tagPreparation
	) {
		return new OnboardingPreferencePlace(
			PlaceProvider.valueOf(row.provider()),
			row.externalPlaceId(),
			livePlace.name(),
			livePlace.address(),
			livePlace.thumbnailUrl(),
			livePlace.category(),
			livePlace.description(),
			tagPreparation == null ? List.of() : tagPreparation.tags(),
			row.sortOrder()
		);
	}

	private BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.VALIDATION_FAILED, message);
	}

	private record PlaceKey(String provider, String externalPlaceId) {
	}
}
