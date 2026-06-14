package com.soomgil.notification.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.api.dto.BulkUpdateResult;
import com.soomgil.notification.api.dto.Notification;
import com.soomgil.notification.api.dto.PagedNotification;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController extends ApiControllerSupport {

	@GetMapping
	public PagedNotification listNotifications(
		@RequestParam(required = false) Boolean unreadOnly,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PatchMapping("/{notificationId}/read")
	public Notification markAsRead(@PathVariable UUID notificationId) {
		return notImplemented();
	}

	@PostMapping("/read-all")
	public BulkUpdateResult markAllAsRead() {
		return notImplemented();
	}

	@DeleteMapping("/{notificationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNotification(@PathVariable UUID notificationId) {
		notImplemented();
	}
}
