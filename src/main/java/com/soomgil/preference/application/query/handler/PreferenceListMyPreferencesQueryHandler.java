package com.soomgil.preference.application.query.handler;

import com.soomgil.preference.api.dto.MyPreferenceSummary;
import com.soomgil.preference.application.query.dto.ListMyPreferencesQuery;
import org.springframework.stereotype.Service;

/**
 * 여행 취향 분석 query를 preference 저장소에서 조회한다.
 */
@Service
public class PreferenceListMyPreferencesQueryHandler implements ListMyPreferencesQueryHandler {

	private final PreferenceUserPreferenceQueryService service;

	public PreferenceListMyPreferencesQueryHandler(PreferenceUserPreferenceQueryService service) {
		this.service = service;
	}

	@Override
	public MyPreferenceSummary handle(ListMyPreferencesQuery query) {
		return service.listMyPreferences();
	}
}
