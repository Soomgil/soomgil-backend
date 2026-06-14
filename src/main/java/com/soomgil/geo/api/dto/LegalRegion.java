package com.soomgil.geo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LegalRegion(
	@NotBlank
	@Size(min = 10, max = 10)
	String code,
	@NotBlank
	String name,
	@NotBlank
	String fullName,
	@NotNull
	LegalRegionLevel level,
	String parentCode,
	@NotNull
	Boolean isActive
) {
}
