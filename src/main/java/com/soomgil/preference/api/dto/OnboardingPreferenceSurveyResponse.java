package com.soomgil.preference.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 현재 활성화된 가입 취향 설문과 사용자의 완료 상태.
 *
 * <p>{@code completedAt}이 null이면 {@code requiredPlaceCount}개의 장소를 모두 평가해야 한다.
 */
public record OnboardingPreferenceSurveyResponse(
	UUID surveyVersionId,
	String code,
	int requiredPlaceCount,
	OffsetDateTime completedAt,
	List<OnboardingPreferencePlace> places
) {
	/**
	 * 사용자가 활성 설문을 이미 완료했는지 반환한다.
	 *
	 * @return 완료 시각이 있으면 {@code true}
	 */
	@JsonProperty("completed")
	public boolean completed() {
		return completedAt != null;
	}
}
