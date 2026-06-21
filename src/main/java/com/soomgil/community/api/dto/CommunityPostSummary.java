package com.soomgil.community.api.dto;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 목록용 요약 응답.
 *
 * <p>피드/검색 결과 등 목록 화면에 필요한 필드만 포함한다.
 * 상세 일정, 미디어, snapshot은 {@link CommunityPostDetail}로 조회한다.
 * {@code moderationStatus}가 VISIBLE인 게시글만 공개 목록에 노출된다.
 *
 * @param id 게시글 식별자
 * @param sourceTripId 원본 여행방 식별자
 * @param publishedBy 게시글 발행자 요약 정보
 * @param coverMedia 대표 미디어 파일
 * @param visibility 공개 범위 (PUBLIC, UNLISTED)
 * @param title 게시글 제목
 * @param summary 게시글 요약
 * @param hashtags 게시글 해시태그 목록
 * @param likeCount 좋아요 수
 * @param retripCount retrip(재여행) 횟수
 * @param commentCount 댓글 수
 * @param mediaCount 첨부 미디어 수
 * @param likedByMe 현재 사용자의 좋아요 여부, 미인증 조회면 null
 * @param moderationStatus 모더레이션 상태
 * @param publishedAt 발행 시각
 */
public record CommunityPostSummary(
	@NotNull
	UUID id,
	UUID sourceTripId,
	@Valid
	UserSummary publishedBy,
	@Valid
	MediaFile coverMedia,
	@NotNull
	PostVisibility visibility,
	@NotBlank
	String title,
	String summary,
	List<String> hashtags,
	Integer likeCount,
	Integer retripCount,
	Integer commentCount,
	Integer mediaCount,
	Boolean likedByMe,
	@NotNull
	ModerationStatus moderationStatus,
	@NotNull
	OffsetDateTime publishedAt
) {
}
