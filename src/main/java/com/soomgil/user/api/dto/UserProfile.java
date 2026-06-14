package com.soomgil.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;

public record UserProfile(
	@NotBlank
	@Size(max = 80)
	String displayName,
	URI profileImageUrl,
	UUID profileMediaFileId,
	@Size(max = 500)
	String bio,
	@NotNull
	UserProfileVisibility profileVisibility
) {
}
