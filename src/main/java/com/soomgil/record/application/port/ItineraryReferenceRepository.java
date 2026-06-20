package com.soomgil.record.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * 여행 기록이 참조하는 일정 day와 item 조회 계약.
 */
public interface ItineraryReferenceRepository {

	boolean existsDay(UUID tripId, UUID dayId);

	Optional<UUID> findItemDayId(UUID tripId, UUID itemId);
}
