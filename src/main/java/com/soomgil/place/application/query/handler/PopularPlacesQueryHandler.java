package com.soomgil.place.application.query.handler;

import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.application.query.dto.PopularPlacesQuery;

public interface PopularPlacesQueryHandler {
	PagedPlaceSummary handle(PopularPlacesQuery query);
}
