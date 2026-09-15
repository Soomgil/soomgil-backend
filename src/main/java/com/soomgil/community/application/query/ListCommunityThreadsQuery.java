package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.PagedCommunityThread;
import java.util.UUID;

/**
 * 커뮤니티 공개 피드를 조회하는 query.
 *
 * <p>공개 피드이므로 {@code viewerUserId}는 null일 수 있고, 이때 likedByMe/editableByMe는 false로 계산된다.
 * 삭제되거나 숨김 처리된 쓰레드는 목록에서 제외한다. 정렬은 {@code createdAt desc, id desc}로 고정하며
 * {@code page}는 0부터 시작하고 {@code size}는 1~100으로 보정된다.
 *
 * @param viewerUserId 조회자. 비로그인 조회면 null
 * @param authorId 특정 작성자만 필터링할 때 사용. 전체 피드면 null
 * @param query 본문 검색어. 필터를 쓰지 않으면 null
 * @param page 0 기반 page 번호
 * @param size page 크기
 */
public record ListCommunityThreadsQuery(
	UUID viewerUserId,
	UUID authorId,
	String query,
	int page,
	int size
) implements Query<PagedCommunityThread> {
}
