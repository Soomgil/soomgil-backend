package com.soomgil.social.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** 현재 사용자에게 들어온 비공개 프로필 follow 요청. */
public record FollowRequest(
	@Valid
	@NotNull
	UserSummary follower,
	@NotBlank
	String status,
	@NotNull
	OffsetDateTime createdAt
) {
}
