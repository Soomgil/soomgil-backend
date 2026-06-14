package com.soomgil.chat.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTripChatMessageRequest(
	@NotBlank
	@Size(min = 1, max = 2000)
	String content
) {
}
