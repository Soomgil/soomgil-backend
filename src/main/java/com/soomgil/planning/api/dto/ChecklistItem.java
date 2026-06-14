package com.soomgil.planning.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChecklistItem(
	@NotNull
	UUID id,
	@NotNull
	UUID checklistId,
	@NotNull
	Integer sortOrder,
	@NotBlank
	String content,
	@Valid
	@NotNull
	List<ChecklistMemberStatus> memberStatuses,
	OffsetDateTime deletedAt
) {
}
