package com.soomgil.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/** 방향성을 가진 사용자 follow 관계와 현재 상태. */
public record SocialFollowRecord(
	UUID followerUserId,
	UUID followingUserId,
	String status,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt
) {
}
