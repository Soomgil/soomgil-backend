package com.soomgil.community.api.dto;

import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 쓰레드 답글 응답.
 *
 * <p>중첩은 1단계까지만 허용하므로 {@code replies}는 {@code depth == 0}인 답글에서만 채워지고,
 * {@code depth == 1}인 답글의 {@code replies}는 항상 빈 목록이다.
 * 삭제/숨김 답글의 {@code content}는 null이며 클라이언트는 tombstone으로 표시한다.
 *
 * @param id 답글 식별자
 * @param threadId 소속 쓰레드 식별자
 * @param parentReplyId 부모 답글 식별자. root 답글이면 null
 * @param author 작성자 공개 요약 정보
 * @param content 본문. 삭제/숨김이면 null
 * @param depth 중첩 깊이. 0은 root 답글, 1은 답글의 답글
 * @param editableByMe 조회자가 수정할 수 있는지 여부
 * @param moderationStatus 모더레이션 상태
 * @param deletedAt soft delete 시각. 삭제되지 않았으면 null
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 수정 시각
 * @param replies 1단계 하위 답글 목록
 */
public record CommunityThreadReply(
	@NotNull
	UUID id,
	@NotNull
	UUID threadId,
	UUID parentReplyId,
	@Valid
	UserSummary author,
	String content,
	@NotNull
	Integer depth,
	boolean editableByMe,
	@NotNull
	ModerationStatus moderationStatus,
	OffsetDateTime deletedAt,
	@NotNull
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt,
	@Valid
	@NotNull
	List<CommunityThreadReply> replies
) {

	public CommunityThreadReply {
		replies = replies == null ? List.of() : List.copyOf(replies);
	}
}
