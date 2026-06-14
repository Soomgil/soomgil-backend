package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunityPostShareTokenResponse(
	@NotNull
	UUID postId,
	@NotBlank
	String shareToken,
	@NotNull
	URI shareUrl,
	@NotNull
	OffsetDateTime rotatedAt
) {
}
