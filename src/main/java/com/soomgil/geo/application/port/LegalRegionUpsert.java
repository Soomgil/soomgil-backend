package com.soomgil.geo.application.port;

import com.soomgil.geo.domain.model.LegalRegionLevel;
import java.time.Instant;
import java.util.Objects;

/**
 * 법정동 지역 upsert 모델.
 */
public record LegalRegionUpsert(
	String code,
	String name,
	String fullName,
	LegalRegionLevel level,
	String parentCode,
	String sidoCode,
	String sigunguCode,
	String eupmyeondongCode,
	String rawStatus,
	boolean active,
	Instant syncedAt
) {

	public LegalRegionUpsert {
		Objects.requireNonNull(code, "code must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(fullName, "fullName must not be null");
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(sidoCode, "sidoCode must not be null");
		Objects.requireNonNull(rawStatus, "rawStatus must not be null");
		Objects.requireNonNull(syncedAt, "syncedAt must not be null");
	}
}
