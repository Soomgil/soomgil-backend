package com.soomgil.community.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateCommunityPostRequest(
	PostVisibility visibility,
	@Size(max = 180)
	String title,
	String summary,
	UUID coverMediaFileId,
	List<UUID> mediaFileIds,
	List<String> hashtags
) {
}
