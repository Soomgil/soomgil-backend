package com.soomgil.social.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

public record UserFollowRecord(
	UUID followerUserId,
	UUID followingUserId,
	String status,
	Instant createdAt,
	Instant updatedAt
) {
}
