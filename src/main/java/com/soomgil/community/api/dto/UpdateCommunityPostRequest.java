package com.soomgil.community.api.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 수정 요청.
 *
 * <p>null 필드는 수정하지 않고 기존 값을 유지한다.
 * {@code visibility}를 UNLISTED로 변경하면 기존 공개 URL이 만료되고 새 shareToken이 발급될 수 있다.
 * 일정 snapshot 자체는 수정할 수 없고 메타데이터만 변경한다.
 *
 * @param visibility 공개 범위 (PUBLIC, UNLISTED)
 * @param title 게시글 제목 (최대 180자)
 * @param summary 게시글 요약
 * @param coverMediaFileId 대표 미디어 파일 식별자
 * @param mediaFileIds 첨부 미디어 파일 식별자 목록
 * @param hashtags 게시글 해시태그 목록
 */
public record UpdateCommunityPostRequest(
	PostVisibility visibility,
	@Size(max = 180)
	String title,
	String summary,
	UUID coverMediaFileId,
	List<UUID> mediaFileIds,
	List<String> hashtags
) {
}
