package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 쓰레드 좋아요 처리 결과.
 *
 * <p>좋아요는 멱등하므로 같은 요청을 반복해도 {@code liked}와 {@code likeCount}는 변하지 않는다.
 *
 * @param threadId 대상 쓰레드 식별자
 * @param liked 요청 처리 후 조회자의 좋아요 상태
 * @param likeCount 처리 후 전체 좋아요 수
 */
public record CommunityThreadReactionSummary(
	@NotNull
	UUID threadId,
	boolean liked,
	int likeCount
) {
}
