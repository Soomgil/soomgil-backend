package com.soomgil.community.application.service;

import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.domain.model.CommunityCommentRecord;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityCommentMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 모더레이션 조치를 신고 대상(게시글 또는 댓글)에 적용한다.
 *
 * <p>{@code HIDE}/{@code RESTORE}는 대상의 {@code moderation_status}를 변경하고,
 * {@code DELETE}는 soft delete를 수행한다. 대상이 존재하지 않거나 이미 삭제된 경우
 * {@link ErrorCode#REPORT_TARGET_NOT_FOUND}를 던진다.
 */
@Component
public class ModerationTargetService {

	private final CommunityPostMapper postMapper;
	private final CommunityCommentMapper commentMapper;

	public ModerationTargetService(CommunityPostMapper postMapper, CommunityCommentMapper commentMapper) {
		this.postMapper = postMapper;
		this.commentMapper = commentMapper;
	}

	/**
	 * 대상이 존재하고 삭제되지 않았는지 검증하고 소유자 userId를 반환한다.
	 *
	 * @param targetType 대상 유형
	 * @param targetId 대상 식별자
	 * @return 대상 소유자 userId
	 * @throws CommunityException 대상이 없거나 삭제된 경우
	 */
	public UUID requireTargetAndReturnOwner(ReportTargetType targetType, UUID targetId) {
		return switch (targetType) {
			case POST -> postMapper.findById(targetId)
				.filter(post -> post.deletedAt() == null)
				.map(CommunityPostRecord::publishedByUserId)
				.orElseThrow(() -> new CommunityException(ErrorCode.REPORT_TARGET_NOT_FOUND));
			case POST_COMMENT -> commentMapper.findById(targetId)
				.filter(comment -> comment.deletedAt() == null)
				.map(CommunityCommentRecord::authorUserId)
				.orElseThrow(() -> new CommunityException(ErrorCode.REPORT_TARGET_NOT_FOUND));
		};
	}

	/**
	 * 모더레이션 조치를 대상에 적용하고 결과 moderation status를 반환한다.
	 *
	 * @param targetType 대상 유형
	 * @param targetId 대상 식별자
	 * @param action 조치 유형
	 * @param reason 사유 (nullable)
	 * @param now 적용 시각
	 * @return 결과 moderation status
	 */
	public ModerationStatus applyAction(
		ReportTargetType targetType, UUID targetId,
		ModerationActionType action, String reason, Instant now
	) {
		return switch (action) {
			case HIDE -> {
				updateModerationStatus(targetType, targetId, ModerationStatus.HIDDEN, reason, now);
				yield ModerationStatus.HIDDEN;
			}
			case RESTORE -> {
				updateModerationStatus(targetType, targetId, ModerationStatus.VISIBLE, reason, now);
				yield ModerationStatus.VISIBLE;
			}
			case DELETE -> {
				softDelete(targetType, targetId, reason, now);
				yield ModerationStatus.DELETED;
			}
		};
	}

	private void updateModerationStatus(
		ReportTargetType targetType, UUID targetId,
		ModerationStatus status, String reason, Instant now
	) {
		if (targetType == ReportTargetType.POST) {
			postMapper.updateModerationStatus(targetId, status, reason, now);
		} else {
			commentMapper.updateModerationStatus(targetId, status, reason, now);
		}
	}

	private void softDelete(ReportTargetType targetType, UUID targetId, String reason, Instant now) {
		if (targetType == ReportTargetType.POST) {
			postMapper.softDelete(targetId, reason, now);
		} else {
			commentMapper.softDelete(targetId, reason, now);
		}
	}
}
