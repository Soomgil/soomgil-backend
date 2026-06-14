package com.soomgil.social.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PagedFollowRequest(
	@Valid
	@NotNull
	List<FollowRequest> items,
	@Valid
	@NotNull
	PageMeta page
) {
}
