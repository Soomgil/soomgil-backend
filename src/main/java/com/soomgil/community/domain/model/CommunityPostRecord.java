package com.soomgil.community.domain.model;

import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import java.time.Instant;
import java.util.UUID;

/**
 * 커뮤니티 게시글 row.
 *
 * <p>여행방({@code sourceTripId})의 특정 version({@code sourceTripVersion})에서 발행된
 * immutable snapshot. 발행 후 내용 수정은 title/summary/visibility/media/hashtags로
 * 제한되고, 본문 snapshot 자체는 변경되지 않는다.
 *
 * @param id 게시글 식별자
 * @param sourceTripId 원본 여행방 식별자
 * @param sourceTripVersion 발행 시점 여행방 version
 * @param publishedByUserId 발행자 식별자
 * @param visibility 공개 범위 (PUBLIC, UNLISTED)
 * @param title 제목
 * @param summary 요약
 * @param coverMediaFileId 표지 미디어 식별자 (nullable)
 * @param snapshotVersion snapshot 버전 (현재는 1 고정)
 * @param shareTokenHash 공유 토큰 SHA-256 hash. UNLISTED 접근 시 사용
 * @param shareTokenCreatedAt 공유 토큰 최초 발급 시각
 * @param shareTokenRotatedAt 공유 토큰 마지막 rotate 시각
 * @param moderationStatus 모더레이션 상태
 * @param publishedAt 발행 시각
 * @param deletedAt soft delete 시각 (null이면 활성)
 */
public record CommunityPostRecord(
	UUID id,
	UUID sourceTripId,
	long sourceTripVersion,
	UUID publishedByUserId,
	PostVisibility visibility,
	String title,
	String summary,
	UUID coverMediaFileId,
	int snapshotVersion,
	String shareTokenHash,
	Instant shareTokenCreatedAt,
	Instant shareTokenRotatedAt,
	ModerationStatus moderationStatus,
	Instant publishedAt,
	Instant deletedAt
) {

	/**
	 * 게시글이 삭제되었는지 확인한다.
	 *
	 * @return deletedAt이 설정되었으면 true
	 */
	public boolean isDeleted() {
		return deletedAt != null;
	}

	/**
	 * 공개 조회(목록/feed)에 노출 가능한 상태인지 확인한다.
	 *
	 * <p>삭제되지 않고, 모더레이션 VISIBLE이고, PUBLIC이어야 한다.
	 *
	 * @return 노출 가능하면 true
	 */
	public boolean isPubliclyVisible() {
		return !isDeleted()
			&& moderationStatus == ModerationStatus.VISIBLE
			&& visibility == PostVisibility.PUBLIC;
	}

	/**
	 * 주어진 사용자가 이 게시글의 발행자인지 확인한다.
	 *
	 * @param userId 확인할 사용자 식별자
	 * @return 발행자면 true
	 */
	public boolean isPublishedBy(UUID userId) {
		return userId != null && userId.equals(publishedByUserId);
	}

	/**
	 * 공유 토큰이 설정되어 있는지 확인한다.
	 *
	 * @return shareTokenHash가 있으면 true
	 */
	public boolean hasShareToken() {
		return shareTokenHash != null && !shareTokenHash.isBlank();
	}
}
