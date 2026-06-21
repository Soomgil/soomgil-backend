package com.soomgil.social.domain.model;

import java.util.UUID;

/**
 * 팔로워 또는 팔로잉 목록에 노출할 최소 사용자 정보.
 *
 * <p>이름과 공개 프로필 이미지만 포함하며 이메일이나 비공개 프로필 필드는 포함하지 않는다.
 */
public record SocialFollowUserRecord(
	UUID userId,
	String displayName,
	String profileImageUrl
) {
}
