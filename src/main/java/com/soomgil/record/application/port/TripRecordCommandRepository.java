package com.soomgil.record.application.port;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 여행 기록 쓰기 persistence 계약.
 */
public interface TripRecordCommandRepository {

	void insertEntry(TripRecordEntryCreate entry);

	void updateEntry(TripRecordEntryUpdate entry);

	boolean softDeleteEntry(UUID tripId, UUID recordId, OffsetDateTime deletedAt);

	void deleteMediaLinks(UUID recordId);

	void insertMediaLinks(UUID recordId, List<UUID> mediaFileIds, OffsetDateTime createdAt);
}
