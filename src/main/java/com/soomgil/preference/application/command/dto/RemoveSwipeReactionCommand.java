package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.place.api.dto.PlaceProvider;

/**
 * 현재 사용자의 장소 반응을 취소하는 command.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 */
public record RemoveSwipeReactionCommand(
	PlaceProvider provider,
	String externalPlaceId
) implements Command<NoResult> {
}
