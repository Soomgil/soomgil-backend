package com.soomgil.auth.application.query;

import com.soomgil.auth.api.dto.PagedSecurityEvent;
import com.soomgil.common.cqrs.Query;
import java.util.UUID;

/**
 * 보안 이벤트 목록 조회 요청.
 *
 * @param userId 사용자 식별자
 * @param page 0부터 시작하는 page 번호
 * @param size page당 항목 수
 */
public record ListSecurityEventsQuery(UUID userId, int page, int size) implements Query<PagedSecurityEvent> {
}
