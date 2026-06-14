package com.soomgil.user.api.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateMeRequest(
	@Size(min = 1, max = 80)
	String displayName,
	UUID profileMediaFileId,
	@Size(max = 500)
	String bio,
	UserProfileVisibility profileVisibility
) {
}
