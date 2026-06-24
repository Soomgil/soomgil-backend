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

	List<TripRecordDayReadModel> findDays(UUID tripId);

	TripRecordPhotoPage findPhotos(UUID tripId, int page, int size);

	TripRecordPhotoPage findPhotosByUser(UUID userId, int page, int size);

	Optional<TripRecordPhotoUrlReadModel> findAccessiblePhotoUrl(UUID userId, UUID mediaFileId);

	/**
	 * 사용자가 소유하거나 active member인 요청 여행의 사진 요약을 한 query로 조회한다.
	 *
	 * <p>접근할 수 없거나 삭제된 여행은 결과에서 제외되며, 사진이 없는 접근 가능 여행은 포함된다.
	 */
	List<TripRecordPhotoSummaryReadModel> findPhotoSummariesByUser(UUID userId, List<UUID> tripIds);
}
