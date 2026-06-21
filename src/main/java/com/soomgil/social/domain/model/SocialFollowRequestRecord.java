package com.soomgil.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/** 비공개 프로필에 들어온 PENDING follow 요청 표시 정보. */
public record SocialFollowRequestRecord(
	UUID followerUserId,
	String displayName,
	String profileImageUrl,
	Instant createdAt
) {
}
