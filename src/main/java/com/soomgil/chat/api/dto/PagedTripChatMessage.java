package com.soomgil.chat.api.dto;

import com.soomgil.common.api.dto.OffsetPageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedTripChatMessage(
	@Valid
	List<TripChatMessage> items,
	@Valid
	OffsetPageMeta page
) {
}
