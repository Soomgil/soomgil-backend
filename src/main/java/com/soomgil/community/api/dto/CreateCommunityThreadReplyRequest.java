package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 답글 작성 요청.
 *
 * <p>{@code parentReplyId}가 있으면 1단계 하위 답글로 생성된다. 이미 1단계인 답글을 부모로 지정하면
 * {@code THREAD_REPLY_DEPTH_EXCEEDED}로 거부된다.
 *
 * @param parentReplyId 부모 답글 식별자. root 답글이면 null
 * @param content 본문 (1~500자)
 */
public record CreateCommunityThreadReplyRequest(
	UUID parentReplyId,
	@NotBlank
	@Size(min = 1, max = 500)
	String content
) {
}
