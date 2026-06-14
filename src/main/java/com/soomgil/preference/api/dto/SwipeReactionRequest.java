package com.soomgil.preference.api.dto;

import jakarta.validation.constraints.NotNull;

public record SwipeReactionRequest(
	@NotNull
	SwipeReaction reaction,
	String source
) {
}
