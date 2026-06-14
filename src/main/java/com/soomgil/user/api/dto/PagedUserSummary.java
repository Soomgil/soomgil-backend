package com.soomgil.user.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PagedUserSummary(
	@Valid
	@NotNull
	List<UserSummary> items,
	@Valid
	@NotNull
	PageMeta page
) {
}
