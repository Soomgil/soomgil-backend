package com.soomgil.social.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 두 사용자 사이의 방향성 follow 관계 응답. */
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
