package com.soomgil.preference.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.preference.api.dto.MyPreferenceSummary;
import com.soomgil.preference.application.query.dto.ListMyPreferencesQuery;

/**
 * 여행 취향 분석 query를 처리하는 application 계약.
 */
public interface ListMyPreferencesQueryHandler
	extends QueryHandler<ListMyPreferencesQuery, MyPreferenceSummary> {
}
