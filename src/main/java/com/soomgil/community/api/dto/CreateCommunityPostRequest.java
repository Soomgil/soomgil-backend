package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateCommunityPostRequest(
	@NotNull
	UUID sourceTripId,
	@NotNull
	Long baseVersion,
	@NotNull
	PostVisibility visibility,
	@NotBlank
	@Size(max = 180)
	String title,
	String summary,
	UUID coverMediaFileId,
	List<UUID> mediaFileIds,
	List<String> hashtags
) {
}
