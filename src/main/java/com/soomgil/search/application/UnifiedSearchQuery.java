package com.soomgil.search.application;

import com.soomgil.common.cqrs.Query;
import com.soomgil.search.api.dto.UnifiedSearchResponse;
import java.util.UUID;

/**
 * 통합 검색 쿼리.
 *
 * @param requesterUserId 검색 요청자 (Trip은 본인 소속 여행방만 검색)
 * @param query           검색 키워드
 * @param size            각 섹션별 최대 결과 수 (기본 4)
 */
public record UnifiedSearchQuery(
	UUID requesterUserId,
	String query,
	int size
) implements Query<UnifiedSearchResponse> {
}
