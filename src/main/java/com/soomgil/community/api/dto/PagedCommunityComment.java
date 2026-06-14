package com.soomgil.community.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedCommunityComment(
	@Valid
	List<CommunityComment> items,
	@Valid
	PageMeta page
) {
}
