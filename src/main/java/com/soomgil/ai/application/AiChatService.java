package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiChatMessage;
import com.soomgil.ai.api.dto.AiChatSession;
import com.soomgil.ai.api.dto.AiMessageResponse;
import com.soomgil.ai.api.dto.AiMessageRole;
import com.soomgil.ai.api.dto.PagedAiChatMessage;
import com.soomgil.ai.infrastructure.persistence.AiChatMapper;
import com.soomgil.ai.infrastructure.persistence.AiChatMessageRow;
import com.soomgil.ai.infrastructure.persistence.AiChatSessionRow;
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.common.api.dto.OffsetPageMeta;
import com.soomgil.geo.api.dto.Viewport;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatService {

	private static final List<String> SORT = List.of("createdAt,desc", "id,desc");
	private final TripAccessGuard accessGuard;
	private final AiChatMapper mapper;
	private final AiGuideModel model;
	private final FindDisplayNameQueryHandler displayNameHandler;
	private final SimpMessagingTemplate messagingTemplate;

	public AiChatService(
		TripAccessGuard accessGuard,
		AiChatMapper mapper,
		AiGuideModel model,
		FindDisplayNameQueryHandler displayNameHandler,
		SimpMessagingTemplate messagingTemplate
	) {
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.mapper = Objects.requireNonNull(mapper);
		this.model = Objects.requireNonNull(model);
		this.displayNameHandler = Objects.requireNonNull(displayNameHandler);
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
	}

	@Transactional
	public AiChatSession getSession(UUID tripId, UUID userId) {
		accessGuard.requireActiveMember(tripId, userId);
		return toDto(requireSession(tripId));
	}

	@Transactional
	public PagedAiChatMessage listMessages(UUID tripId, UUID userId, int offset, int limit) {
		accessGuard.requireActiveMember(tripId, userId);
		validatePage(offset, limit);
		AiChatSessionRow session = requireSession(tripId);
		List<AiChatMessageRow> rows = mapper.findMessages(session.id(), offset, limit + 1);
		boolean hasMore = rows.size() > limit;
		return new PagedAiChatMessage(
			rows.stream().limit(limit).map(this::toDto).toList(),
			new OffsetPageMeta(offset, limit, hasMore ? offset + limit : null, hasMore, SORT)
		);
	}

	public AiMessageResponse createMessage(
		UUID tripId,
		UUID userId,
		String content,
		Long baseVersion
	) {
		return createMessage(tripId, userId, content, baseVersion, null);
	}

	public AiMessageResponse createMessage(
		UUID tripId,
		UUID userId,
		String content,
		Long baseVersion,
		Viewport viewport
	) {
		accessGuard.requireActiveMember(tripId, userId);
		String question = content == null ? "" : content.trim();
		if (question.isEmpty() || question.length() > 4000) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		AiChatSessionRow session = requireSession(tripId);
		List<AiGuideRequest.AiGuideTurn> recent = new ArrayList<>(mapper.findRecentMessages(session.id(), 20).stream()
			.map(row -> new AiGuideRequest.AiGuideTurn(row.role(), row.content()))
			.toList());
		mapper.insertMessage(UUID.randomUUID(), session.id(), userId, AiMessageRole.USER.name(), question, Instant.now());
		String answer = model.reply(new AiGuideRequest(
			tripId, userId, session.summary(), recent, question, baseVersion, viewport
		));
		if (answer == null || answer.isBlank()) {
			throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "AI provider returned an empty response.");
		}
		UUID assistantMessageId = UUID.randomUUID();
		mapper.insertMessage(
			assistantMessageId, session.id(), null, AiMessageRole.ASSISTANT.name(), answer.trim(), Instant.now()
		);
		AiChatMessage assistant = toDto(mapper.findMessageById(assistantMessageId));
		AiMessageResponse response = new AiMessageResponse(assistant, List.of(), baseVersion, false, false);
		messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/ai", response);
		return response;
	}

	private AiChatSessionRow requireSession(UUID tripId) {
		AiChatSessionRow existing = mapper.findSessionByTripId(tripId);
		if (existing != null) {
			return existing;
		}
		mapper.insertSessionIfAbsent(UUID.randomUUID(), tripId, Instant.now());
		AiChatSessionRow created = mapper.findSessionByTripId(tripId);
		if (created == null) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI session could not be created.");
		}
		return created;
	}

	private void validatePage(int offset, int limit) {
		if (offset < 0 || limit < 1 || limit > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "offset or limit is out of range.");
		}
	}

	private AiChatSession toDto(AiChatSessionRow row) {
		return new AiChatSession(row.id(), row.tripId(), row.status(), offset(row.summaryUpdatedAt()), offset(row.createdAt()));
	}

	private AiChatMessage toDto(AiChatMessageRow row) {
		if (row == null) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Stored AI message could not be loaded.");
		}
		UserSummary requester = null;
		if (row.requesterUserId() != null) {
			FindDisplayNameQuery query = new FindDisplayNameQuery(row.requesterUserId());
			String name = row.requesterDisplayName() != null
				? row.requesterDisplayName() : displayNameHandler.handle(query);
			URI image = row.requesterProfileImageUrl() != null
				? URI.create(row.requesterProfileImageUrl()) : displayNameHandler.findProfileImageUrl(query);
			requester = new UserSummary(row.requesterUserId(), name, image);
		}
		return new AiChatMessage(
			row.id(), AiMessageRole.valueOf(row.role()), requester, row.content(), row.toolCallId(), offset(row.createdAt())
		);
	}

	private OffsetDateTime offset(Instant value) {
		return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}
}
