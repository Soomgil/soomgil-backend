package com.soomgil.preference.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SwipeFeedItem(
	@Valid
	@NotNull
	SwipeFeedPlace place,
	SwipeReaction myReaction,
	@Valid
	List<UserSummary> likedByFollowees
) {
}
