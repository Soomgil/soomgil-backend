package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import java.time.OffsetDateTime;

/**
 * 현재 사용자의 장소 스와이프 최종 반응을 저장하는 command.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param reaction 저장할 최종 반응
 * @param sourceModifiedAt 반응 대상 장소 원천 수정 시각
 */
public record UpsertSwipeReactionCommand(
	PlaceProvider provider,
	String externalPlaceId,
	SwipeReaction reaction,
	OffsetDateTime sourceModifiedAt
) implements Command<SwipeReactionResponse> {
}
