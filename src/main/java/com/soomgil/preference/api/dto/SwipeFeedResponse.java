package com.soomgil.preference.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SwipeFeedResponse(
	@Valid
	@NotNull
	List<SwipeFeedItem> items,
	String nextSeed
) {
}
