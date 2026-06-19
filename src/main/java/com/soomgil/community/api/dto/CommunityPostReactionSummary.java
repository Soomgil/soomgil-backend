package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 게시글 반응(좋아요) 요약 응답.
 *
 * <p>현재 사용자의 좋아요 여부와 전체 좋아요 수를 함께 반환한다.
 * 비인증 조회 시 {@code liked}는 false로 처리된다.
 *
 * @param postId 게시글 식별자
 * @param liked 현재 사용자의 좋아요 여부
 * @param likeCount 전체 좋아요 수
 */
public record CommunityPostReactionSummary(
	@NotNull
	UUID postId,
	@NotNull
	Boolean liked,
	@NotNull
	Integer likeCount
) {
}
