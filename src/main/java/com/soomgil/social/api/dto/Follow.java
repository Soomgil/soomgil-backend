package com.soomgil.social.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Follow(
	@NotNull
	UUID followerUserId,
	@NotNull
	UUID followingUserId,
	@NotNull
	FollowStatus status,
	@NotNull
	OffsetDateTime createdAt
) {
}
