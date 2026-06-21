package com.soomgil.notification.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.api.dto.BulkUpdateResult;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.notification.application.NotificationService;
import com.soomgil.notification.api.dto.Notification;
import com.soomgil.notification.api.dto.PagedNotification;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController extends ApiControllerSupport {
	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public PagedNotification listNotifications(
		@RequestParam(required = false) Boolean unreadOnly,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return notificationService.list(currentUser.userId(), Boolean.TRUE.equals(unreadOnly), page, size);
	}

	@PatchMapping("/{notificationId}/read")
	public Notification markAsRead(
		@PathVariable UUID notificationId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return notificationService.markRead(currentUser.userId(), notificationId);
	}

	@PatchMapping("/read-all")
	public BulkUpdateResult markAllAsRead(@AuthenticationPrincipal CurrentUser currentUser) {
		return notificationService.markAllRead(currentUser.userId());
	}

	@DeleteMapping("/{notificationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNotification(
		@PathVariable UUID notificationId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		notificationService.delete(currentUser.userId(), notificationId);
	}
}
