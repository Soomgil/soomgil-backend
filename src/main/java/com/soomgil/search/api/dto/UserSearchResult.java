package com.soomgil.search.api.dto;

import java.net.URI;
import java.util.UUID;

/**
 * 통합 검색 결과의 사용자 항목.
 *
 * <p>{@code UserSummary}에 팔로워 수를 더해, 검색 결과 정렬 기준으로 활용한다.
 */
public record UserSearchResult(
	UUID id,
	String displayName,
	URI profileImageUrl,
	long followerCount
) {
}
