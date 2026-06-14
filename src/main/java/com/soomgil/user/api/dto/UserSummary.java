package com.soomgil.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

public record UserSummary(
	@NotNull
	UUID id,
	@NotBlank
	String displayName,
	URI profileImageUrl
) {
}
