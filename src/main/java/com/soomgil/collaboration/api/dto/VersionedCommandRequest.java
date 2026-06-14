package com.soomgil.collaboration.api.dto;

import jakarta.validation.constraints.NotNull;

public record VersionedCommandRequest(
	@NotNull
	Long baseVersion
) {
}
