package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 가입 설문 관광지 하나에 대한 사용자의 최종 반응.
 *
 * <p>일반 취향 수집과 같은 세 방향 제스처를 사용하므로
 * {@link SwipeReaction#LIKE}, {@link SwipeReaction#NOPE}, {@link SwipeReaction#SUPER_LIKE}를 허용한다.
 */
public record OnboardingPreferenceAnswer(
	@NotNull PlaceProvider provider,
	@NotBlank String externalPlaceId,
	@NotNull SwipeReaction reaction
) {
}
