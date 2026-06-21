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
	User user,
	/** 온보딩 완료 여부. false면 프론트가 /register?oauth=1로 라우팅해 추가 정보를 받는다. */
	boolean onboarded
) {
}
