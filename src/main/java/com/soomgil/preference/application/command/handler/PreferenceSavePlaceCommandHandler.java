package com.soomgil.preference.application.command.handler;

import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.preference.application.command.dto.SavePlaceCommand;
import org.springframework.stereotype.Service;

/**
 * 저장 장소 생성 command를 preference 저장소에 반영한다.
 */
@Service
public class PreferenceSavePlaceCommandHandler implements SavePlaceCommandHandler {

	private final PreferenceSavedPlaceService service;

	public PreferenceSavePlaceCommandHandler(PreferenceSavedPlaceService service) {
		this.service = service;
	}

	@Override
	public SavedPlace handle(SavePlaceCommand command) {
		return service.save(command);
	}
}
