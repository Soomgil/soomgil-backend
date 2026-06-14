package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record OAuthAuthorizationUrlResponse(
	@NotNull
	URI authorizationUrl,
	@NotBlank
	String state
) {
}
