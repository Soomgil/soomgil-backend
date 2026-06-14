package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCommunityCommentRequest(
	UUID parentCommentId,
	@NotBlank
	@Size(min = 1, max = 2000)
	String content
) {
}
