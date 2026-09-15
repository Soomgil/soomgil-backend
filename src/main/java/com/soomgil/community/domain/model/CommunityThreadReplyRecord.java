package com.soomgil.community.domain.model;

import com.soomgil.community.api.dto.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 답글 row.
 *
 * <p>{@code depth}는 0이 root 답글, 1이 답글의 답글이다. 2단계 이상은
 * {@link com.soomgil.community.domain.policy.CommunityThreadPolicy}와 DB CHECK 제약으로 차단한다.
 *
 * @param id 답글 식별자
 * @param threadId 소속 쓰레드 식별자
 * @param parentReplyId 부모 답글 식별자. root 답글이면 null
 * @param authorUserId 작성자
 * @param content 본문 원문
 * @param depth 중첩 깊이
 * @param moderationStatus 모더레이션 상태
 * @param deletedAt soft delete 시각. null이면 활성
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 수정 시각
 */
public record CommunityThreadReplyRecord(
	UUID id,
	UUID threadId,
	UUID parentReplyId,
	UUID authorUserId,
	String content,
	int depth,
	ModerationStatus moderationStatus,
	Instant deletedAt,
	Instant createdAt,
	Instant updatedAt
) {

	/**
	 * soft delete 여부.
	 *
	 * @return 삭제되었으면 true
	 */
	public boolean isDeleted() {
		return deletedAt != null;
	}

	/**
	 * 이 답글을 해당 사용자가 작성했는지 확인한다.
	 *
	 * @param userId 확인할 사용자. null이면 항상 false
	 * @return 작성자면 true
	 */
	public boolean isAuthoredBy(UUID userId) {
		return userId != null && userId.equals(authorUserId);
	}

	/**
	 * 본문을 공개해도 되는 상태인지 확인한다.
	 *
	 * @return 삭제되지 않고 moderation 상태가 VISIBLE이면 true
	 */
	public boolean isPubliclyReadable() {
		return !isDeleted() && moderationStatus == ModerationStatus.VISIBLE;
	}
}
