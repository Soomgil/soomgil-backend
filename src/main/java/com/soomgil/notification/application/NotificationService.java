package com.soomgil.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.BulkUpdateResult;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.notification.api.dto.Notification;
import com.soomgil.notification.api.dto.PagedNotification;
import com.soomgil.notification.api.dto.TripInviteNotificationPayload;
import com.soomgil.notification.infrastructure.persistence.NotificationMapper;
import com.soomgil.notification.infrastructure.persistence.NotificationRow;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 현재 사용자에게 속한 알림의 조회, 읽음 처리, 삭제를 담당한다. */
@Service
public class NotificationService {

	private static final List<String> SORT = List.of("createdAt,desc", "id,desc");
	private final NotificationMapper mapper;
	private final ObjectMapper objectMapper;

	public NotificationService(NotificationMapper mapper, ObjectMapper objectMapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Transactional(readOnly = true)
	public PagedNotification list(UUID userId, boolean unreadOnly, int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page or size is out of range.");
		}
		long total = mapper.countByRecipient(userId, unreadOnly);
		List<Notification> items = mapper.findByRecipient(userId, unreadOnly, page * size, size)
			.stream().map(this::toDto).toList();
		int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
		return new PagedNotification(items, new PageMeta(page, size, total, totalPages, SORT));
	}

	@Transactional
	public Notification markRead(UUID userId, UUID notificationId) {
		if (mapper.markRead(notificationId, userId, Instant.now()) == 0) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification was not found.");
		}
		return toDto(requireOwned(userId, notificationId));
	}

	@Transactional
	public BulkUpdateResult markAllRead(UUID userId) {
		return new BulkUpdateResult(mapper.markAllRead(userId, Instant.now()));
	}

	@Transactional
	public void delete(UUID userId, UUID notificationId) {
		if (mapper.deleteOwned(notificationId, userId) == 0) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification was not found.");
		}
	}

	private NotificationRow requireOwned(UUID userId, UUID notificationId) {
		NotificationRow row = mapper.findOwned(notificationId, userId);
		if (row == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification was not found.");
		}
		return row;
	}

	private Notification toDto(NotificationRow row) {
		UserSummary actor = row.actorUserId() == null ? null : new UserSummary(
			row.actorUserId(), row.actorDisplayName(), uri(row.actorProfileImageUrl())
		);
		TripInviteNotificationPayload payload;
		try {
			payload = objectMapper.readValue(row.payloadJson(), TripInviteNotificationPayload.class);
		}
		catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new IllegalStateException("Notification payload is invalid.", exception);
		}
		return new Notification(
			row.id(), actor, payload.tripId(), row.type(), row.title(), row.body(), payload,
			offset(row.readAt()), offset(row.createdAt())
		);
	}

	private URI uri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}

	private OffsetDateTime offset(Instant value) {
		return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}
}
