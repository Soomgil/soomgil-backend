package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.application.command.UpdateCommunityPostCommand;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.domain.model.HashtagRecord;
import com.soomgil.community.domain.policy.CommunityPostPolicy;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.HashtagMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostHashtagMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostMediaMapper;
import com.soomgil.global.error.ErrorCode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글을 수정한다.
 *
 * <p>snapshot 본문(sourceTripId, sourceTripVersion)은 변경할 수 없다. 수정 가능한 항목은
 * title, summary, visibility, coverMediaFileId, mediaFileIds, hashtags 뿐이다.
 * {@code null} 필드는 "변경 없음", 빈 리스트는 "전체 삭제"를 의미한다.
 *
 * <p>해시태그/미디어는 기존 연결을 전부 지우고 새로 삽입하는 replace 전략을 사용한다.
 */
@Component
@Transactional
public class UpdateCommunityPostCommandHandler
	implements CommandHandler<UpdateCommunityPostCommand, CommunityPostDetail> {

	private final CommunityPostMapper postMapper;
	private final HashtagMapper hashtagMapper;
	private final PostHashtagMapper postHashtagMapper;
	private final PostMediaMapper postMediaMapper;
	private final CommunityPostAssembler assembler;

	public UpdateCommunityPostCommandHandler(
		CommunityPostMapper postMapper,
		HashtagMapper hashtagMapper,
		PostHashtagMapper postHashtagMapper,
		PostMediaMapper postMediaMapper,
		CommunityPostAssembler assembler
	) {
		this.postMapper = postMapper;
		this.hashtagMapper = hashtagMapper;
		this.postHashtagMapper = postHashtagMapper;
		this.postMediaMapper = postMediaMapper;
		this.assembler = assembler;
	}

	@Override
	public CommunityPostDetail handle(UpdateCommunityPostCommand command) {
		CommunityPostRecord post = postMapper.findById(command.postId())
			.filter(p -> !p.isDeleted())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		if (!post.isPublishedBy(command.actorUserId())) {
			throw new CommunityException(ErrorCode.POST_AUTHOR_REQUIRED);
		}

		Instant now = Instant.now();
		String effectiveTitle = command.title() != null ? command.title() : post.title();
		String effectiveSummary = command.summary() != null ? command.summary() : post.summary();
		PostVisibility effectiveVisibility =
			command.visibility() != null ? command.visibility() : post.visibility();
		UUID effectiveCoverMedia = command.coverMediaFileId() != null
			? command.coverMediaFileId()
			: post.coverMediaFileId();

		validateInputs(effectiveTitle, effectiveSummary, command.mediaFileIds(), command.hashtags());

		boolean basicsChanged = command.title() != null
			|| command.summary() != null
			|| command.visibility() != null
			|| command.coverMediaFileId() != null;
		if (basicsChanged) {
			postMapper.updateBasics(
				command.postId(),
				effectiveTitle,
				effectiveSummary,
				effectiveVisibility,
				effectiveCoverMedia,
				now
			);
		}

		if (command.mediaFileIds() != null) {
			postMediaMapper.deleteAllByPostId(command.postId());
			linkMedia(command.postId(), command.mediaFileIds(), now);
		}

		if (command.hashtags() != null) {
			replaceHashtags(command.postId(), command.hashtags(), now);
		}

		CommunityPostRecord updated = postMapper.findById(command.postId())
			.orElseThrow(() -> new CommunityException(ErrorCode.INTERNAL_ERROR));
		return assembler.toDetail(updated, command.actorUserId(), null, true);
	}

	private void validateInputs(
		String title, String summary, List<UUID> mediaFileIds, List<String> hashtags
	) {
		if (!CommunityPostPolicy.isValidTitle(title)) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (!CommunityPostPolicy.isValidSummary(summary)) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (mediaFileIds != null && mediaFileIds.size() > CommunityPostPolicy.MEDIA_MAX_COUNT) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (hashtags != null && hashtags.size() > CommunityPostPolicy.HASHTAG_MAX_COUNT) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
	}

	private void replaceHashtags(UUID postId, List<String> rawHashtags, Instant now) {
		// 기존 연결의 usage_count를 먼저 감소시키고 링크를 삭제한다.
		List<UUID> oldHashtagIds = postHashtagMapper.findHashtagIdsByPostId(postId);
		for (UUID oldId : oldHashtagIds) {
			hashtagMapper.adjustUsageCount(oldId, -1, now);
		}
		postHashtagMapper.deleteAllByPostId(postId);

		if (rawHashtags.isEmpty()) {
			return;
		}

		Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
		for (String raw : rawHashtags) {
			String normalized = HashtagRecord.normalize(raw);
			if (normalized == null || normalized.length() > CommunityPostPolicy.HASHTAG_NAME_MAX) {
				continue;
			}
			normalizedToOriginal.putIfAbsent(normalized, raw.trim());
		}
		if (normalizedToOriginal.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String> entry : normalizedToOriginal.entrySet()) {
			String normalized = entry.getKey();
			String original = entry.getValue();
			hashtagMapper.insertOrIgnore(UUID.randomUUID(), original, normalized, now);
			HashtagRecord hashtag = hashtagMapper.findByNormalizedName(normalized)
				.orElseThrow(() -> new CommunityException(ErrorCode.INTERNAL_ERROR));
			postHashtagMapper.insert(postId, hashtag.id(), now);
			hashtagMapper.adjustUsageCount(hashtag.id(), 1, now);
		}
	}

	private void linkMedia(UUID postId, List<UUID> mediaFileIds, Instant now) {
		int sortOrder = 0;
		for (UUID mediaFileId : mediaFileIds) {
			postMediaMapper.insert(UUID.randomUUID(), postId, mediaFileId, sortOrder, null, now);
			sortOrder++;
		}
	}
}
