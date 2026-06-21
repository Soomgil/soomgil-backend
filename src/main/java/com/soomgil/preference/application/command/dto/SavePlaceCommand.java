package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SavedPlace;

/**
 * 현재 사용자의 SUPER_LIKE 장소 저장을 요청하는 command.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 */
public record SavePlaceCommand(
	PlaceProvider provider,
	String externalPlaceId
) implements Command<SavedPlace> {
}
