package com.soomgil.collaboration.infrastructure.websocket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 활성 WebSocket session과 인증 사용자 소유 관계를 관리한다.
 */
@Component
public class CollaborationWebSocketSessionRegistry {

	private final ConcurrentHashMap<String, UUID> sessions = new ConcurrentHashMap<>();

	public void register(String sessionId, UUID userId) {
		if (sessionId != null && userId != null) {
			sessions.put(sessionId, userId);
		}
	}

	public void unregister(String sessionId) {
		if (sessionId != null) {
			sessions.remove(sessionId);
		}
	}

	public boolean isOwnedBy(String sessionId, UUID userId) {
		return userId != null && userId.equals(sessions.get(sessionId));
	}
}
