package com.soomgil.community.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 생성 요청.
 *
 * <p>{@code sourceTripId}의 여행방을 snapshot으로 발행한다.
 * {@code baseVersion}은 발행 기준이 되는 여행방 버전이며, 동시 수정 충돌 감지에 사용된다.
 * {@code visibility}가 UNLISTED인 경우 shareToken이 발급되어 공유 링크로만 접근할 수 있다.
 *
 * @param sourceTripId 원본 여행방 식별자
 * @param baseVersion 발행 기준 여행방 버전
 * @param visibility 공개 범위 (PUBLIC, UNLISTED)
 * @param title 게시글 제목 (최대 180자)
 * @param summary 게시글 요약
 * @param coverMediaFileId 대표 미디어 파일 식별자
 * @param mediaFileIds 첨부 미디어 파일 식별자 목록
 * @param hashtags 게시글 해시태그 목록
 */
public record CreateCommunityPostRequest(
	@NotNull
	UUID sourceTripId,
	@NotNull
	Long baseVersion,
	@NotNull
	PostVisibility visibility,
	@NotBlank
	@Size(max = 180)
	String title,
	String summary,
	UUID coverMediaFileId,
	List<UUID> mediaFileIds,
	List<String> hashtags
) {
}
