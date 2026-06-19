package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.NoResult;
import com.soomgil.preference.application.command.dto.UnsavePlaceCommand;
import org.springframework.stereotype.Service;

/**
 * 저장 장소 해제 command를 preference 저장소에 반영한다.
 */
@Service
public class PreferenceUnsavePlaceCommandHandler implements UnsavePlaceCommandHandler {

	private final PreferenceSavedPlaceService service;

	public PreferenceUnsavePlaceCommandHandler(PreferenceSavedPlaceService service) {
		this.service = service;
	}

	@Override
	public NoResult handle(UnsavePlaceCommand command) {
		return service.unsave(command);
	}
}
