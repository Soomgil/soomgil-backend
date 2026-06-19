package com.soomgil.preference.application.query.handler;

import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.application.command.handler.PreferenceSavedPlaceService;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;
import org.springframework.stereotype.Service;

/**
 * 현재 사용자의 저장 장소 목록 query를 preference 저장소에서 조회한다.
 */
@Service
public class PreferenceListSavedPlacesQueryHandler implements ListSavedPlacesQueryHandler {

	private final PreferenceSavedPlaceService service;

	public PreferenceListSavedPlacesQueryHandler(PreferenceSavedPlaceService service) {
		this.service = service;
	}

	@Override
	public PagedSavedPlace handle(ListSavedPlacesQuery query) {
		return service.list(query);
	}
}
