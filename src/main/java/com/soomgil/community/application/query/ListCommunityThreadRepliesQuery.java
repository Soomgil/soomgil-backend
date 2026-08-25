package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.PagedCommunityThreadReply;
import java.util.UUID;

/**
 * 쓰레드 답글 목록을 조회하는 query.
 *
 * <p>page 단위는 root 답글이며, 각 root 답글의 1단계 하위 답글은 함께 반환되지만
 * page 계산에는 포함되지 않는다. 정렬은 {@code createdAt asc, id asc}로 고정한다.
 *
 * @param threadId 대상 쓰레드 식별자
 * @param viewerUserId 조회자. 비로그인 조회면 null
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record ListCommunityThreadRepliesQuery(
	UUID threadId,
	UUID viewerUserId,
	int page,
	int size
) implements Query<PagedCommunityThreadReply> {
}
