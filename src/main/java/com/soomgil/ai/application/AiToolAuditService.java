package com.soomgil.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.ai.api.dto.AiToolCallStatus;
import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.ai.infrastructure.persistence.AiChatMapper;
import com.soomgil.collaboration.application.port.CollaborationSessionIdProvider;
import com.soomgil.global.error.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiToolAuditService {

	private final AiChatMapper mapper;
	private final ObjectMapper objectMapper;
	private final CollaborationSessionIdProvider sessionIdProvider;

	public AiToolAuditService(
		AiChatMapper mapper,
		ObjectMapper objectMapper,
		CollaborationSessionIdProvider sessionIdProvider
	) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
		this.sessionIdProvider = sessionIdProvider;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public UUID start(AiGuideRequest request, String toolName, AiToolExecutionPolicy policy, Object arguments, Long versionBefore) {
		UUID id = UUID.randomUUID();
		mapper.insertToolCall(
			id, request.sessionId(), request.tripId(), request.requestMessageId(), request.requesterUserId(),
			sessionIdProvider.currentSessionId(), toolName, policy.name(), json(arguments), versionBefore, Instant.now()
		);
		return id;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiToolCall succeed(
		UUID id, String toolName, AiToolExecutionPolicy policy, Object result,
		Long versionBefore, Long versionAfter, boolean undoAvailable
	) {
		mapper.completeToolCall(id, json(result), versionAfter, undoAvailable, Instant.now());
		return new AiToolCall(
			id, toolName, policy, AiToolCallStatus.SUCCEEDED,
			versionBefore, versionAfter, undoAvailable, null
		);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void fail(UUID id, RuntimeException exception) {
		String code = exception instanceof BusinessException business
			? business.errorCode().code() : "AI_TOOL_EXECUTION_FAILED";
		String message = exception.getMessage();
		if (message != null && message.length() > 500) {
			message = message.substring(0, 500);
		}
		mapper.failToolCall(id, code, message, Instant.now());
	}

	public boolean hasCollaborationSession() {
		return sessionIdProvider.currentSessionId() != null;
	}

	private String json(Object value) {
		try {
			JsonNode tree = objectMapper.valueToTree(value);
			maskSecrets(tree);
			return objectMapper.writeValueAsString(tree);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("AI tool audit payload could not be serialized.", exception);
		}
	}

	private void maskSecrets(JsonNode node) {
		if (node == null) {
			return;
		}
		if (node instanceof ObjectNode object) {
			object.properties().forEach(entry -> {
				String key = entry.getKey().toLowerCase();
				if (key.contains("token") || key.contains("secret") || key.contains("password")
					|| key.contains("authorization") || key.endsWith("key")) {
					object.put(entry.getKey(), "[REDACTED]");
				}
				else {
					maskSecrets(entry.getValue());
				}
			});
			return;
		}
		if (node.isArray()) {
			node.forEach(this::maskSecrets);
		}
	}
}
