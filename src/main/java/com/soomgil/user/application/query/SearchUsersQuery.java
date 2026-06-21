package com.soomgil.user.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.user.api.dto.PagedUserSummary;

/**
 * 사용자 검색 요청.
 *
 * <p>{@code q}는 {@code auth.user_profiles.display_name}에 대한 부분 일치 검색어다.
 * 빈 값이면 전체 목록의 첫 page를 반환한다. {@code PRIVATE} 프로필은 검색 결과에서 제외한다.
 *
 * @param query 검색어. {@code null} 또는 빈 값이면 전체 조회
 * @param page 0부터 시작하는 page 번호
 * @param size page 당 항목 수
 */
public record SearchUsersQuery(
	String query,
	int page,
	int size
) implements Query<PagedUserSummary> {
}
