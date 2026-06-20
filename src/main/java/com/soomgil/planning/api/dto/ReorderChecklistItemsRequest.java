package com.soomgil.planning.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderChecklistItemsRequest(
	@Valid
	@NotNull
	List<ChecklistItemOrder> itemOrders
) {
}
