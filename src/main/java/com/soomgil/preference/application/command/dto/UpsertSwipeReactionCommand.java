package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.domain.policy.PreferenceSource;
import java.time.OffsetDateTime;

/**
 * 현재 사용자의 장소 스와이프 최종 반응을 저장하는 command.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param reaction 저장할 최종 반응
 * @param sourceModifiedAt 반응 대상 장소 원천 수정 시각
 * @param source 취향 근거가 생성된 제품 경험
 * @param sourceResourceId 설문 버전 등 source 안에서 반응을 식별하는 값
 */
public record UpsertSwipeReactionCommand(
	PlaceProvider provider,
	String externalPlaceId,
	SwipeReaction reaction,
	OffsetDateTime sourceModifiedAt,
	PreferenceSource source,
	String sourceResourceId
) implements Command<SwipeReactionResponse> {

	public UpsertSwipeReactionCommand(
		PlaceProvider provider,
		String externalPlaceId,
		SwipeReaction reaction,
		OffsetDateTime sourceModifiedAt
	) {
		this(provider, externalPlaceId, reaction, sourceModifiedAt, PreferenceSource.HOME_BACKGROUND, null);
	}
}
