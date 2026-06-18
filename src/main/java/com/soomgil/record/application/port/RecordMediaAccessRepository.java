package com.soomgil.record.application.port;

import java.util.List;
import java.util.UUID;

/**
 * 여행 기록에 연결할 미디어 접근 권한 조회 계약.
 */
public interface RecordMediaAccessRepository {

	boolean areLinkable(UUID recordId, UUID userId, List<UUID> mediaFileIds);
}
