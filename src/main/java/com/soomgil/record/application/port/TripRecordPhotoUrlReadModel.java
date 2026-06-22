package com.soomgil.record.application.port;

import java.util.UUID;

/**
 * 접근 가능한 여행 기록 사진의 URL 재발급용 query 결과.
 */
public record TripRecordPhotoUrlReadModel(
	UUID mediaFileId,
	String objectKey,
	String publicUrl
) {
}
