package com.soomgil.collaboration.infrastructure.web;

import com.soomgil.collaboration.application.port.CollaborationSessionIdProvider;
import com.soomgil.collaboration.infrastructure.websocket.CollaborationWebSocketSessionRegistry;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 현재 HTTP 요청 헤더에서 협업 WebSocket session ID를 읽는다.
 */
@Component
public class HttpCollaborationSessionIdProvider implements CollaborationSessionIdProvider {

	public static final String SESSION_HEADER = "X-Soomgil-WebSocket-Session-Id";
	private final CollaborationWebSocketSessionRegistry sessionRegistry;

	public HttpCollaborationSessionIdProvider(CollaborationWebSocketSessionRegistry sessionRegistry) {
		this.sessionRegistry = Objects.requireNonNull(sessionRegistry, "sessionRegistry must not be null");
	}

	@Override
	public String currentSessionId() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return null;
		}
		HttpServletRequest request = attributes.getRequest();
		String value = request.getHeader(SESSION_HEADER);
		if (value == null || value.isBlank()) {
			return null;
		}
		return ownedSessionOrNull(value, request.getUserPrincipal());
	}

	public String requireOwnedSession(String sessionId, Principal principal) {
		String normalized = sessionId == null ? null : sessionId.trim();
		if (normalized == null || normalized.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "WebSocket session ID is required.");
		}
		UUID userId = parseUserId(principal);
		if (!sessionRegistry.isOwnedBy(normalized, userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "WebSocket session does not belong to the current user.");
		}
		return normalized;
	}

	private String ownedSessionOrNull(String sessionId, Principal principal) {
		String normalized = sessionId == null ? null : sessionId.trim();
		if (normalized == null || normalized.isBlank()) {
			return null;
		}
		UUID userId = parseUserIdOrNull(principal);
		if (userId == null || !sessionRegistry.isOwnedBy(normalized, userId)) {
			return null;
		}
		return normalized;
	}

	private UUID parseUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is required.");
		}
		try {
			return UUID.fromString(principal.getName());
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is invalid.");
		}
	}

	private UUID parseUserIdOrNull(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(principal.getName());
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}
}
