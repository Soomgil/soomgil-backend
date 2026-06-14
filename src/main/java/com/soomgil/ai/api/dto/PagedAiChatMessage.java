package com.soomgil.ai.api.dto;

import com.soomgil.common.api.dto.OffsetPageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedAiChatMessage(
	@Valid
	List<AiChatMessage> items,
	@Valid
	OffsetPageMeta page
) {
}
