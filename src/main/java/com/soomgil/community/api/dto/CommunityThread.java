package com.soomgil.community.api.dto;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.user.api.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 커뮤니티 공개 피드의 쓰레드 응답. 목록과 상세가 같은 표현을 사용한다.
 *
 * <p>{@code content}는 삭제되거나 숨김 처리된 쓰레드에서 null이 되며, 클라이언트는 이 경우
 * tombstone UI를 그려야 한다. {@code likedByMe}와 {@code editableByMe}는 비로그인 조회에서 항상 false다.
 * {@code likeCount}와 {@code replyCount}는 denormalized counter가 아니라 조회 시점 집계값이다.
 *
 * @param id 쓰레드 식별자
 * @param author 작성자 공개 요약 정보
 * @param content 본문. 삭제/숨김이면 null
 * @param media 첨부 이미지 목록. 없으면 빈 목록
 * @param likeCount 현재 좋아요 수
 * @param replyCount 노출 가능한 답글 수
 * @param likedByMe 조회자가 좋아요를 눌렀는지 여부
 * @param editableByMe 조회자가 수정/삭제할 수 있는지 여부
 * @param moderationStatus 모더레이션 상태
 * @param deletedAt soft delete 시각. 삭제되지 않았으면 null
 * @param createdAt 생성 시각
 * @param updatedAt 마지막 수정 시각
 */
public record CommunityThread(
	@NotNull
	UUID id,
	@Valid
	UserSummary author,
	String content,
	@Valid
	@NotNull
	List<MediaFile> media,
	int likeCount,
	int replyCount,
	boolean likedByMe,
	boolean editableByMe,
	@NotNull
	ModerationStatus moderationStatus,
	OffsetDateTime deletedAt,
	@NotNull
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {

	public CommunityThread {
		media = media == null ? List.of() : List.copyOf(media);
	}
}
