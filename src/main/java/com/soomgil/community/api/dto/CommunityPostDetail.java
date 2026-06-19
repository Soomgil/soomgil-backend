package com.soomgil.community.api.dto;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 게시글 상세 응답.
 *
 * <p>발행자가 여행방에서 생성한 immutable snapshot과 발행 메타데이터를 포함한다.
 * 비공개(UNLISTED) 게시글은 {@code shareToken}을 통해서만 접근할 수 있으며,
 * {@code snapshotVersion}은 여행방 변경과 무관하게 발행 시점의 내용을 고정한다.
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
 * @param snapshotVersion 발행 시점의 snapshot 버전
 * @param snapshot 발행 시점의 여행 일정 snapshot
 * @param media 첨부 미디어 파일 목록
 * @param shareToken UNLISTED 게시글 접근용 토큰, PUBLIC이면 null
 * @param shareUrl 공유 URL, UNLISTED가 아니면 null일 수 있다
 * @param shareTokenCreatedAt 공유 토큰 생성 시각
 * @param shareTokenRotatedAt 공유 토큰 최근 갱신 시각
 */
public record CommunityPostDetail(
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
	OffsetDateTime publishedAt,
	@NotNull
	Integer snapshotVersion,
	@Valid
	@NotNull
	CommunityPostSnapshot snapshot,
	@Valid
	List<MediaFile> media,
	String shareToken,
	URI shareUrl,
	OffsetDateTime shareTokenCreatedAt,
	OffsetDateTime shareTokenRotatedAt
) {
}
