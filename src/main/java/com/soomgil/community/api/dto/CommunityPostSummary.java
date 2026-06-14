package com.soomgil.community.api.dto;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CommunityPostSummary(
	@NotNull
	UUID id,
	UUID sourceTripId,
	@Valid
	UserSummary publishedBy,
	@Valid
	MediaFile coverMedia,
	@NotNull
	PostVisibility visibility,
	@NotBlank
	String title,
	String summary,
	List<String> hashtags,
	Integer likeCount,
	Integer retripCount,
	Integer commentCount,
	Integer mediaCount,
	Boolean likedByMe,
	@NotNull
	ModerationStatus moderationStatus,
	@NotNull
	OffsetDateTime publishedAt
) {
}
