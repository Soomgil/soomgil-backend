package com.soomgil.record.application.port;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행 기록 사진 조회 모델.
 */
public record TripRecordPhotoReadModel(
	UUID tripId,
	String tripTitle,
	UUID recordId,
	UUID itineraryDayId,
	UUID itineraryItemId,
	UUID uploadedByUserId,
	UUID mediaFileId,
	URI publicUrl,
	String mimeType,
	Long byteSize,
	Integer width,
	Integer height,
	String mediaStatus,
	OffsetDateTime mediaCreatedAt,
	OffsetDateTime takenAt,
	OffsetDateTime createdAt
) {
}
