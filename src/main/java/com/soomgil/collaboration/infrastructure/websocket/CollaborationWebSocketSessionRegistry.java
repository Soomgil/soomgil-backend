package com.soomgil.collaboration.infrastructure.websocket;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 활성 WebSocket session과 인증 사용자 소유 관계를 관리한다.
 */
@Component
public class CollaborationWebSocketSessionRegistry {

	private final ConcurrentHashMap<String, UUID> sessions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Set<UUID>> sessionTripIds = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, UUID>> tripSessionUsers = new ConcurrentHashMap<>();

	public void register(String sessionId, UUID userId) {
		if (sessionId != null && userId != null) {
			sessions.put(sessionId, userId);
		}
	}

	public void registerTripPresence(String sessionId, UUID tripId, UUID userId) {
		if (sessionId == null || tripId == null || userId == null) {
			return;
		}
		register(sessionId, userId);
		sessionTripIds.computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet()).add(tripId);
		tripSessionUsers.computeIfAbsent(tripId, ignored -> new ConcurrentHashMap<>()).put(sessionId, userId);
	}

	public Map<UUID, List<UUID>> unregister(String sessionId) {
		if (sessionId == null) {
			return Map.of();
		}
		sessions.remove(sessionId);
		Set<UUID> tripIds = sessionTripIds.remove(sessionId);
		if (tripIds == null || tripIds.isEmpty()) {
			return Map.of();
		}

		Map<UUID, List<UUID>> snapshots = new LinkedHashMap<>();
		for (UUID tripId : tripIds) {
			ConcurrentHashMap<String, UUID> tripSessions = tripSessionUsers.get(tripId);
			if (tripSessions == null) {
				snapshots.put(tripId, List.of());
				continue;
			}
			tripSessions.remove(sessionId);
			List<UUID> activeUserIds = activeUserIdsFrom(tripSessions);
			if (tripSessions.isEmpty()) {
				tripSessionUsers.remove(tripId, tripSessions);
			}
			snapshots.put(tripId, activeUserIds);
		}
		return snapshots;
	}

	public boolean isOwnedBy(String sessionId, UUID userId) {
		return userId != null && userId.equals(sessions.get(sessionId));
	}

	public Optional<UUID> findUserId(String sessionId) {
		return Optional.ofNullable(sessionId == null ? null : sessions.get(sessionId));
	}

	public List<UUID> activeUserIds(UUID tripId) {
		ConcurrentHashMap<String, UUID> tripSessions = tripId == null ? null : tripSessionUsers.get(tripId);
		return tripSessions == null ? List.of() : activeUserIdsFrom(tripSessions);
	}

	private List<UUID> activeUserIdsFrom(Map<String, UUID> tripSessions) {
		return tripSessions.values().stream()
			.distinct()
			.sorted(Comparator.comparing(UUID::toString))
			.toList();
	}
}
