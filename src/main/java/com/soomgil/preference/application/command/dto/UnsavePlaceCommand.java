package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.place.api.dto.PlaceProvider;

/**
 * 현재 사용자의 저장 장소 해제를 요청하는 command.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 */
public record UnsavePlaceCommand(
	PlaceProvider provider,
	String externalPlaceId
) implements Command<NoResult> {
}
