package com.soomgil.auth.api.dto;

public record LogoutRequest(
	String refreshToken,
	Boolean allDevices
) {
}
