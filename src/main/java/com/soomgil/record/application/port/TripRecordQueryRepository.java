package com.soomgil.record.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 여행 기록 읽기 persistence 계약.
 */
public interface TripRecordQueryRepository {

	TripRecordPage findEntries(UUID tripId, int page, int size);

	Optional<TripRecordEntryReadModel> findEntry(UUID tripId, UUID recordId);

	List<TripRecordMediaReadModel> findMedia(UUID recordId);

	TripRecordPhotoPage findPhotos(UUID tripId, int page, int size);

	TripRecordPhotoPage findPhotosByUser(UUID userId, int page, int size);
}
