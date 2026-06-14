package com.soomgil.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record RegisterRequest(
	@NotBlank
	@Email
	@Size(max = 320)
	String email,
	@NotBlank
	@Size(min = 8, max = 128)
	String password,
	@NotBlank
	@Size(min = 1, max = 80)
	String displayName,
	@Size(max = 12)
	String displayLanguage,
	@Size(max = 50)
	String timezone,
	Boolean marketingEmailOptIn,
	@NotNull
	@Size(min = 1)
	List<UUID> acceptedPolicyDocumentIds
) {
}
