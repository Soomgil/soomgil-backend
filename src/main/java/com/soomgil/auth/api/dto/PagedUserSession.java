package com.soomgil.auth.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PagedUserSession(
	@Valid
	@NotNull
	List<UserSession> items,
	@Valid
	@NotNull
	PageMeta page
) {
}
