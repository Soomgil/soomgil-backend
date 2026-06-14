package com.soomgil.social.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

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
