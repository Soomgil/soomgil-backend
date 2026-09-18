package com.soomgil.preference.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 가입 취향 설문 완료 결과.
 */
public record OnboardingPreferenceCompletionResponse(
	UUID surveyVersionId,
	OffsetDateTime completedAt
) {
}
