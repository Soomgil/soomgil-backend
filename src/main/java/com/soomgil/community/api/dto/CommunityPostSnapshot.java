package com.soomgil.community.api.dto;

import com.soomgil.itinerary.api.dto.ItineraryDay;
import com.soomgil.itinerary.api.dto.RouteSegment;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;

public record CommunityPostSnapshot(
	@Valid
	List<ItineraryDay> days,
	@Valid
	List<RouteSegment> routes,
	@Valid
	UserSummary authorDisplay
) {
}
