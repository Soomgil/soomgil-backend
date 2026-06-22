package com.soomgil.trip.application.query.handler;

import com.soomgil.trip.api.dto.NearestTripDto;
import com.soomgil.trip.application.query.dto.FindNearestTripQuery;

public interface FindNearestTripHandler {
	NearestTripDto handle(FindNearestTripQuery query);
}
