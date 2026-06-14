package com.soomgil.auth.api.dto;

import com.soomgil.user.api.dto.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthTokenResponse(
	@NotBlank
	String accessToken,
	@NotBlank
	String refreshToken,
	@NotBlank
	String tokenType,
	Integer expiresIn,
	@Valid
	@NotNull
	User user
) {
}
