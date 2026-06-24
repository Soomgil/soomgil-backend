package com.soomgil.preference.api.dto;

import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlaceRecommendation(
	@Valid
	@NotNull
	PlaceSummary place,
	@Valid
	@NotNull
	List<UserSummary> matchedMembers,
	int matchedMemberCount,
	int totalMemberCount,
	Integer rank,
	Double distanceMeters,
	String recommendationReason,
	Integer matchPercentage
) {
}
