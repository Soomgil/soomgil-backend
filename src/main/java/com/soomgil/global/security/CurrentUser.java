package com.soomgil.global.security;

import java.util.Objects;
import java.util.UUID;

public record CurrentUser(
	UUID userId,
	String email
) {

	public CurrentUser {
		Objects.requireNonNull(userId, "userId must not be null");
	}
}
