package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunityComment(
	@NotNull
	UUID id,
	@NotNull
	UUID postId,
	UUID parentCommentId,
	@Valid
	@NotNull
	UserSummary author,
	String content,
	@NotNull
	Integer depth,
	@NotNull
	ModerationStatus moderationStatus,
	OffsetDateTime deletedAt,
	@NotNull
	OffsetDateTime createdAt
) {
}
