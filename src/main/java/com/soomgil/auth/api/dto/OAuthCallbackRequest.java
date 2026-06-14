package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record OAuthCallbackRequest(
	@NotBlank
	String code,
	@NotNull
	URI redirectUri,
	String state,
	String nonce
) {
}
