package com.soomgil.global.security;

import java.util.UUID;

@FunctionalInterface
public interface CurrentUserProvider {

	CurrentUser currentUser();

	default UUID currentUserId() {
		return currentUser().userId();
	}
}
