package com.soomgil.user.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserSettingsRequest(
	@Size(max = 12)
	String displayLanguage,
	@Size(max = 50)
	String timezone,
	Boolean marketingEmailOptIn,
	Boolean tripInviteEmailOptIn
) {
}
