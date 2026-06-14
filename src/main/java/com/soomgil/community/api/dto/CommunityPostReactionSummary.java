package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CommunityPostReactionSummary(
	@NotNull
	UUID postId,
	@NotNull
	Boolean liked,
	@NotNull
	Integer likeCount
) {
}
