package com.soomgil.community.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.PostVisibility;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 발행 요청.
 *
 * @param sourceTripId 원본 여행방 식별자
 * @param baseVersion 발행자가 읽은 여행방 version (낙관적 동시성)
 * @param publishedByUserId 발행자
 * @param visibility 공개 범위
 * @param title 제목
 * @param summary 요약 (nullable)
 * @param coverMediaFileId 표지 미디어 (nullable)
 * @param mediaFileIds 본문 미디어 식별자 목록 (nullable, 순서 보존)
 * @param hashtags 해시태그 원본 목록 (nullable, 정규화는 서버에서)
 */
public record CreateCommunityPostCommand(
	UUID sourceTripId,
	long baseVersion,
	UUID publishedByUserId,
	PostVisibility visibility,
	String title,
	String summary,
	UUID coverMediaFileId,
	List<UUID> mediaFileIds,
	List<String> hashtags
) implements Command<CommunityPostDetail> {
}
