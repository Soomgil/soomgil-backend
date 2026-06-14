package com.soomgil.user.api.dto;

import com.soomgil.social.api.dto.FollowStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record UserPublicProfile(
	@NotNull
	UUID id,
	@NotBlank
	String displayName,
	URI profileImageUrl,
	String bio,
	@Min(0)
	Integer followerCount,
	@Min(0)
	Integer followingCount,
	Boolean followedByMe,
	FollowStatus followStatus,
	UserProfileVisibility profileVisibility
) {
}
