package com.soomgil.preference.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 활성 가입 취향 설문의 전체 응답을 원자적으로 제출하는 요청.
 *
 * <p>부분 제출은 허용하지 않으며 활성 version에 속한 장소 10개가 중복 없이 모두 포함되어야 한다.
 */
public record CompleteOnboardingPreferenceSurveyRequest(
	@NotNull UUID surveyVersionId,
	@NotNull @Size(min = 10, max = 10) List<@Valid OnboardingPreferenceAnswer> responses
) {
}
