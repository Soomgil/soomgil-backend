package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.PostVisibility;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 수정 요청.
 *
 * <p>{@code null} 필드는 "변경 없음"을 의미한다. 빈 리스트는 "전체 삭제"를 의미.
 *
 * @param postId 게시글 식별자
 * @param actorUserId 수정 요청자 (발행자 본인이어야 함)
 * @param title 제목 (nullable = 변경 없음)
 * @param summary 요약 (nullable = 변경 없음)
 * @param visibility 공개 범위 (nullable = 변경 없음)
 * @param coverMediaFileId 표지 미디어 (nullable = 변경 없음)
 * @param mediaFileIds 본문 미디어 (null = 변경 없음, 빈 리스트 = 전체 삭제)
 * @param hashtags 해시태그 (null = 변경 없음, 빈 리스트 = 전체 삭제)
 */
public record UpdateCommunityPostCommand(
	UUID postId,
	UUID actorUserId,
	String title,
	String summary,
	PostVisibility visibility,
	UUID coverMediaFileId,
	List<UUID> mediaFileIds,
	List<String> hashtags
) implements Command<CommunityPostDetail> {
}
