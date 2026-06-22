package com.soomgil.record.application.port;

import java.util.UUID;

/**
 * 여러 여행의 기록 사진 집계 query 결과.
 *
 * <p>사진이 없는 접근 가능 여행도 {@code photoCount=0}, cover 필드가 null인 결과로 포함한다.
 */
public record TripRecordPhotoSummaryReadModel(
	UUID tripId,
	long photoCount,
	UUID coverMediaFileId,
	String coverObjectKey,
	String coverPublicUrl
) {
}
