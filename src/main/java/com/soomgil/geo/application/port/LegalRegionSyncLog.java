package com.soomgil.geo.application.port;

import java.time.Instant;
import java.util.Objects;

/**
 * 법정동 동기화 이력 저장 모델.
 */
public record LegalRegionSyncLog(
	String source,
	String sourceFileName,
	int totalCount,
	int insertedCount,
	int updatedCount,
	int deactivatedCount,
	Instant startedAt,
	Instant finishedAt,
	String status,
	String errorMessage
) {

	public LegalRegionSyncLog {
		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(startedAt, "startedAt must not be null");
		Objects.requireNonNull(status, "status must not be null");
	}
}
