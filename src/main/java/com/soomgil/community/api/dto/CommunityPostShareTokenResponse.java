package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 게시글 공유 토큰 응답.
 *
 * <p>UNLISTED 게시글을 외부에 공유할 때 사용하는 토큰과 URL을 반환한다.
 * 토큰이 노출되면 누구나 게시글에 접근할 수 있으므로 비공개 채널로만 전달해야 한다.
 *
 * @param postId 게시글 식별자
 * @param shareToken 공유 접근용 토큰
 * @param shareUrl 공유 URL
 * @param rotatedAt 토큰 최근 갱신 시각
 */
public record CommunityPostShareTokenResponse(
	@NotNull
	UUID postId,
	@NotBlank
	String shareToken,
	@NotNull
	URI shareUrl,
	@NotNull
	OffsetDateTime rotatedAt
) {
}
