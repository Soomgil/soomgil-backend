package com.soomgil.preference.api;

import com.soomgil.global.security.CurrentUser;
import com.soomgil.preference.api.dto.CompleteOnboardingPreferenceSurveyRequest;
import com.soomgil.preference.api.dto.OnboardingPreferenceCompletionResponse;
import com.soomgil.preference.api.dto.OnboardingPreferenceSurveyResponse;
import com.soomgil.preference.application.service.OnboardingPreferenceService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증된 신규 사용자의 필수 관광지 취향 설문 API.
 */
@Validated
@RestController
@RequestMapping("/api/v1/onboarding/preference-survey")
public class OnboardingPreferenceController {

	private final OnboardingPreferenceService service;

	public OnboardingPreferenceController(OnboardingPreferenceService service) {
		this.service = service;
	}

	/**
	 * 활성 가입 취향 설문과 현재 사용자의 완료 상태를 조회한다.
	 *
	 * @param currentUser 인증된 사용자
	 * @return 정렬된 관광지 10개와 완료 상태
	 */
	@GetMapping
	public OnboardingPreferenceSurveyResponse getSurvey(
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser
	) {
		return service.getSurvey(currentUser.userId());
	}

	/**
	 * 관광지 10개의 전체 응답을 저장하고 가입 취향 설문을 완료한다.
	 *
	 * @param currentUser 인증된 사용자
	 * @param request 활성 설문의 전체 응답
	 * @return 설문 version과 완료 시각
	 */
	@PutMapping("/responses")
	public OnboardingPreferenceCompletionResponse complete(
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CompleteOnboardingPreferenceSurveyRequest request
	) {
		return service.complete(currentUser.userId(), request);
	}
}
