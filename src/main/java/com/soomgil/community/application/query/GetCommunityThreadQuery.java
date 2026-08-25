package com.soomgil.community.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.community.api.dto.CommunityThread;
import java.util.UUID;

/**
 * 쓰레드 단건을 조회하는 query.
 *
 * <p>공개 조회이므로 {@code viewerUserId}는 null일 수 있다. 삭제되거나 숨김 처리된 쓰레드는
 * {@code THREAD_NOT_FOUND}로 응답해 존재 여부를 은닉한다.
 *
 * @param threadId 조회할 쓰레드 식별자
 * @param viewerUserId 조회자. 비로그인 조회면 null
 */
public record GetCommunityThreadQuery(
	UUID threadId,
	UUID viewerUserId
) implements Query<CommunityThread> {
}
