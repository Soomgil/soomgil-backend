package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.application.command.CreateCommunityPostCommand;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.application.service.ShareTokenService;
import com.soomgil.community.application.service.TripSnapshotChecker;
import com.soomgil.community.domain.model.CommunityException;
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
 * 커뮤니티 게시글을 발행한다.
 *
 * <p>여행방 version을 검증하고(trip 모듈 미구축이라 stub), 게시글 row를 INSERT한 뒤
 * 해시태그/미디어 연결을 추가한다. UNLISTED 게시글은 즉시 공유 토큰을 발급한다.
 *
 * <p>snapshot은 발행 시점에 고정(immutable)이며, 이후 PATCH로 변경할 수 없다.
 */
@Component
@Transactional
public class CreateCommunityPostCommandHandler
	implements CommandHandler<CreateCommunityPostCommand, CommunityPostDetail> {

	private static final int SNAPSHOT_VERSION_INITIAL = 1;

	private final CommunityPostMapper postMapper;
	private final HashtagMapper hashtagMapper;
	private final PostHashtagMapper postHashtagMapper;
	private final PostMediaMapper postMediaMapper;
	private final ShareTokenService shareTokenService;
	private final TripSnapshotChecker tripSnapshotChecker;
	private final CommunityPostAssembler assembler;

	public CreateCommunityPostCommandHandler(
		CommunityPostMapper postMapper,
		HashtagMapper hashtagMapper,
		PostHashtagMapper postHashtagMapper,
		PostMediaMapper postMediaMapper,
		ShareTokenService shareTokenService,
		TripSnapshotChecker tripSnapshotChecker,
		CommunityPostAssembler assembler
	) {
		this.postMapper = postMapper;
		this.hashtagMapper = hashtagMapper;
		this.postHashtagMapper = postHashtagMapper;
		this.postMediaMapper = postMediaMapper;
		this.shareTokenService = shareTokenService;
		this.tripSnapshotChecker = tripSnapshotChecker;
		this.assembler = assembler;
	}

	@Override
	public CommunityPostDetail handle(CreateCommunityPostCommand command) {
		validateInputs(command);

		// trip 모듈 구축 전까지 stub이 권한/version 검증을 건너뛰고 빈 snapshot 반환.
		// 실제 구현체에서는 TRIP_MEMBER_REQUIRED 또는 SOURCE_TRIP_VERSION_CONFLICT를 던질 수 있다.
		tripSnapshotChecker.fetchSnapshot(
			command.sourceTripId(), command.baseVersion(), command.publishedByUserId()
		);

		Instant now = Instant.now();
		UUID postId = UUID.randomUUID();

		String rawShareToken = null;
		String shareTokenHash = null;
		Instant shareTokenCreatedAt = null;
		if (command.visibility() == PostVisibility.UNLISTED) {
			ShareTokenService.IssuedShareToken issued = shareTokenService.issue();
			rawShareToken = issued.raw();
			shareTokenHash = issued.hash();
			shareTokenCreatedAt = now;
		}

		postMapper.insert(
			postId,
			command.sourceTripId(),
			command.baseVersion(),
			command.publishedByUserId(),
			command.visibility(),
			command.title(),
			command.summary(),
			command.coverMediaFileId(),
			SNAPSHOT_VERSION_INITIAL,
			shareTokenHash,
			shareTokenCreatedAt,
			shareTokenCreatedAt,
			now
		);

		linkHashtags(postId, command.hashtags(), now);
		linkMedia(postId, command.mediaFileIds(), now);

		String issuedRawToken = rawShareToken;
		return postMapper.findById(postId)
			.map(post -> assembler.toDetail(post, command.publishedByUserId(), issuedRawToken, true))
			.orElseThrow(() -> new CommunityException(ErrorCode.INTERNAL_ERROR));
	}

	private void validateInputs(CreateCommunityPostCommand command) {
		if (!CommunityPostPolicy.isValidTitle(command.title())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (!CommunityPostPolicy.isValidSummary(command.summary())) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (command.mediaFileIds() != null && command.mediaFileIds().size() > CommunityPostPolicy.MEDIA_MAX_COUNT) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
		if (command.hashtags() != null && command.hashtags().size() > CommunityPostPolicy.HASHTAG_MAX_COUNT) {
			throw new CommunityException(ErrorCode.VALIDATION_FAILED);
		}
	}

	private void linkHashtags(UUID postId, List<String> rawHashtags, Instant now) {
		if (rawHashtags == null || rawHashtags.isEmpty()) {
			return;
		}
		// 입력 순서를 보존하면서 중복 제거
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
		if (mediaFileIds == null || mediaFileIds.isEmpty()) {
			return;
		}
		int sortOrder = 0;
		for (UUID mediaFileId : mediaFileIds) {
			postMediaMapper.insert(UUID.randomUUID(), postId, mediaFileId, sortOrder, null, now);
			sortOrder++;
		}
	}
}
