package com.soomgil.auth.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PagedSecurityEvent(
	@Valid
	@NotNull
	List<SecurityEvent> items,
	@Valid
	@NotNull
	PageMeta page
) {
}
