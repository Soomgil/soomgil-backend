package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record OnboardRequest(
	@NotBlank
	@Size(min = 1, max = 80)
	String displayName,
	@NotNull
	@Size(min = 1)
	List<UUID> acceptedPolicyDocumentIds
) {
}
