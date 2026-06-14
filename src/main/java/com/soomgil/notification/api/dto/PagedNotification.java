package com.soomgil.notification.api.dto;

import com.soomgil.common.api.dto.PageMeta;
import jakarta.validation.Valid;
import java.util.List;

public record PagedNotification(
	@Valid
	List<Notification> items,
	@Valid
	PageMeta page
) {
}
