package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 가입 설문 관광지 하나에 대한 사용자의 최종 반응.
 *
 * <p>가입 설문에서는 {@link SwipeReaction#LIKE}와 {@link SwipeReaction#NOPE}만 허용한다.
 */
public record OnboardingPreferenceAnswer(
	@NotNull PlaceProvider provider,
	@NotBlank String externalPlaceId,
	@NotNull SwipeReaction reaction
) {
}
