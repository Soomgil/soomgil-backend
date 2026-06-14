package com.soomgil.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UserSettings(
	@NotBlank
	@Size(max = 12)
	String displayLanguage,
	@NotBlank
	@Size(max = 50)
	String timezone,
	@NotNull
	Boolean marketingEmailOptIn,
	OffsetDateTime marketingEmailOptedInAt,
	OffsetDateTime marketingEmailOptedOutAt,
	@NotNull
	Boolean tripInviteEmailOptIn
) {
}
