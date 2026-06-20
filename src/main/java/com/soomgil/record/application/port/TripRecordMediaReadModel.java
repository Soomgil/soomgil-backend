package com.soomgil.record.application.port;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 여행 기록에 연결된 media 조회 모델.
 */
public record TripRecordMediaReadModel(
	UUID recordEntryId,
	UUID mediaFileId,
	URI publicUrl,
	String mimeType,
	Long byteSize,
	Integer width,
	Integer height,
	String status,
	OffsetDateTime createdAt,
	Integer sortOrder
) {
}
