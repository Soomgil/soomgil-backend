package com.soomgil.collaboration.infrastructure.websocket;

import com.soomgil.collaboration.application.port.MapObjectLease;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** 단일 backend 인스턴스에서 15초 지도 오브젝트 lease를 원자적으로 관리한다. */
@Component
public class InMemoryMapObjectLeaseStore implements MapObjectLeaseStore {

	private static final Duration LEASE_DURATION = Duration.ofSeconds(15);
	private final ConcurrentHashMap<LeaseKey, MapObjectLease> leases = new ConcurrentHashMap<>();

	@Override
	public MapObjectLease acquire(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now) {
		requireArguments(tripId, drawingId, userId, sessionId, now);
		LeaseKey key = new LeaseKey(tripId, drawingId);
		MapObjectLease lease = leases.compute(key, (ignored, current) -> {
			if (current != null && current.expiresAt().isAfter(now) && !ownedBy(current, userId, sessionId)) {
				throw new BusinessException(ErrorCode.CONFLICT, "Map object is being edited by another user.");
			}
			return new MapObjectLease(tripId, drawingId, userId, sessionId, now.plus(LEASE_DURATION));
		});
		return lease;
	}

	@Override
	public MapObjectLease renew(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now) {
		requireOwned(tripId, drawingId, userId, sessionId, now);
		return acquire(tripId, drawingId, userId, sessionId, now);
	}

	@Override
	public boolean release(UUID tripId, UUID drawingId, UUID userId, String sessionId) {
		LeaseKey key = new LeaseKey(tripId, drawingId);
		AtomicBoolean released = new AtomicBoolean(false);
		leases.computeIfPresent(key, (ignored, current) -> {
			if (ownedBy(current, userId, sessionId)) {
				released.set(true);
				return null;
			}
			return current;
		});
		return released.get();
	}

	@Override
	public List<MapObjectLease> releaseSession(String sessionId) {
		if (sessionId == null || sessionId.isBlank()) {
			return List.of();
		}
		List<MapObjectLease> released = new ArrayList<>();
		leases.forEach((key, lease) -> {
			if (sessionId.equals(lease.websocketSessionId()) && leases.remove(key, lease)) {
				released.add(lease);
			}
		});
		return List.copyOf(released);
	}

	@Override
	public Optional<MapObjectLease> find(UUID tripId, UUID drawingId, Instant now) {
		LeaseKey key = new LeaseKey(tripId, drawingId);
		MapObjectLease lease = leases.get(key);
		if (lease != null && !lease.expiresAt().isAfter(now)) {
			leases.remove(key, lease);
			lease = null;
		}
		return Optional.ofNullable(lease);
	}

	@Override
	public void requireOwned(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now) {
		MapObjectLease lease = find(tripId, drawingId, now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "An active map object lease is required."));
		if (!ownedBy(lease, userId, sessionId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Map object lease belongs to another session.");
		}
	}

	private boolean ownedBy(MapObjectLease lease, UUID userId, String sessionId) {
		return lease.userId().equals(userId) && lease.websocketSessionId().equals(sessionId);
	}

	private void requireArguments(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now) {
		if (tripId == null || drawingId == null || userId == null || sessionId == null || sessionId.isBlank() || now == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Map object lease fields are required.");
		}
	}

	private record LeaseKey(UUID tripId, UUID drawingId) {
	}
}
