package com.soomgil.collaboration.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 지도 오브젝트 lease의 저장 경계.
 *
 * <p>MVP 구현은 단일 인스턴스 메모리를 사용하며 다중 인스턴스 전환 시 Redis 구현으로 교체한다.
 */
public interface MapObjectLeaseStore {

	MapObjectLease acquire(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now);

	MapObjectLease renew(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now);

	boolean release(UUID tripId, UUID drawingId, UUID userId, String sessionId);

	List<MapObjectLease> releaseSession(String sessionId);

	Optional<MapObjectLease> find(UUID tripId, UUID drawingId, Instant now);

	void requireOwned(UUID tripId, UUID drawingId, UUID userId, String sessionId, Instant now);
}
