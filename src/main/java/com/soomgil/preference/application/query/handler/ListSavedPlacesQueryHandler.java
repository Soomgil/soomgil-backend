package com.soomgil.preference.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;

/**
 * 저장 장소 목록 query를 처리하는 application 계약.
 */
public interface ListSavedPlacesQueryHandler extends QueryHandler<ListSavedPlacesQuery, PagedSavedPlace> {
}
