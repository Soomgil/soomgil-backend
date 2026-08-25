package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 커뮤니티 쓰레드 답글 수정 요청. 답글 작성자만 호출할 수 있다.
 *
 * @param content 새 본문 (1~500자)
 */
public record UpdateCommunityThreadReplyRequest(
	@NotBlank
	@Size(min = 1, max = 500)
	String content
) {
}
