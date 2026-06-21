package com.soomgil.chat.application;

import com.soomgil.chat.api.dto.PagedTripChatMessage;
import com.soomgil.chat.api.dto.TripChatMessage;
import com.soomgil.chat.infrastructure.persistence.TripChatMessageMapper;
import com.soomgil.chat.infrastructure.persistence.TripChatMessageRow;
import com.soomgil.common.api.dto.OffsetPageMeta;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 여행방 멤버 권한을 적용한 채팅 목록, 생성, 본인 메시지 삭제를 처리한다. */
@Service
public class TripChatService {

	private static final List<String> SORT = List.of("createdAt,desc", "id,desc");
	private final TripAccessGuard accessGuard;
	private final TripChatMessageMapper mapper;
	private final SimpMessagingTemplate messagingTemplate;

	public TripChatService(
		TripAccessGuard accessGuard,
		TripChatMessageMapper mapper,
		SimpMessagingTemplate messagingTemplate
	) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
	}

	@Transactional(readOnly = true)
	public PagedTripChatMessage list(UUID tripId, UUID userId, int offset, int limit) {
		accessGuard.requireActiveMember(tripId, userId);
		if (offset < 0 || limit < 1 || limit > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "offset or limit is out of range.");
		}
		List<TripChatMessageRow> rows = mapper.findByTrip(tripId, offset, limit + 1);
		boolean hasMore = rows.size() > limit;
		List<TripChatMessage> items = rows.stream().limit(limit).map(this::toDto).toList();
		return new PagedTripChatMessage(
			items,
			new OffsetPageMeta(offset, limit, hasMore ? offset + limit : null, hasMore, SORT)
		);
	}

	@Transactional
	public TripChatMessage create(UUID tripId, UUID userId, String content) {
		accessGuard.requireActiveMember(tripId, userId);
		String normalized = content == null ? "" : content.trim();
		if (normalized.isEmpty() || normalized.length() > 2000) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		UUID messageId = UUID.randomUUID();
		mapper.insert(messageId, tripId, userId, normalized, Instant.now());
		TripChatMessage message = toDto(requireMessage(messageId));
		broadcastAfterCommit(tripId, message);
		return message;
	}

	@Transactional
	public void delete(UUID tripId, UUID messageId, UUID userId) {
		accessGuard.requireActiveMember(tripId, userId);
		TripChatMessageRow row = requireMessage(messageId);
		if (!tripId.equals(row.tripId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		if (!userId.equals(row.senderUserId())) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Only the sender can delete a chat message.");
		}
		if (row.deletedAt() == null) {
			mapper.softDelete(tripId, messageId, userId, Instant.now());
		}
	}

	private TripChatMessageRow requireMessage(UUID messageId) {
		TripChatMessageRow row = mapper.findById(messageId);
		if (row == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chat message was not found.");
		}
		return row;
	}

	private TripChatMessage toDto(TripChatMessageRow row) {
		URI profileImage = row.senderProfileImageUrl() == null ? null : URI.create(row.senderProfileImageUrl());
		return new TripChatMessage(
			row.id(), row.tripId(), new UserSummary(row.senderUserId(), row.senderDisplayName(), profileImage),
			row.deletedAt() == null ? row.content() : null, offset(row.deletedAt()), offset(row.createdAt())
		);
	}

	private OffsetDateTime offset(Instant value) {
		return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}

	private void broadcastAfterCommit(UUID tripId, TripChatMessage message) {
		Runnable send = () -> messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/chat", message);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send.run();
				}
			});
			return;
		}
		send.run();
	}
}
