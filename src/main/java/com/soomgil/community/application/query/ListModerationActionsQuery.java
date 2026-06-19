package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.PagedModerationAction;

/**
 * 모더레이션 조치 이력 조회 요청 (모더레이터 전용).
 *
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기
 */
public record ListModerationActionsQuery(
	int page,
	int size
) implements Query<PagedModerationAction> {
}
