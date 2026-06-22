package com.soomgil.global.storage;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 권한 확인을 마친 클라이언트가 object를 읽을 때 사용하는 제한 시간 서명 정보. */
public record PresignedStorageRead(
	URI readUrl,
	OffsetDateTime expiresAt
) {
	public PresignedStorageRead {
		Objects.requireNonNull(readUrl, "readUrl must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
	}
}
