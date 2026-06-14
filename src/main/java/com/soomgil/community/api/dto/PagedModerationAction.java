package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedModerationAction(
	@Valid
	List<ModerationAction> items,
	@Valid
	PageMeta page
) {
}
