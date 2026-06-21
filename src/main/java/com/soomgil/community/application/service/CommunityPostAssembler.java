package com.soomgil.community.application.service;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.api.dto.CommunityComment;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.api.dto.CommunityPostSummary;
import com.soomgil.community.domain.model.CommunityCommentRecord;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityCommentMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostHashtagMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostLikeMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostMediaMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostRetripMapper;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.media.application.MediaFileQueryService;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@link CommunityPostRecord}를 API 응답 DTO로 조립한다.
 *
 * <p>발행자 display name, 해시태그, 커버/갤러리 미디어, 좋아요 수,
 * likedByMe, 댓글 수 등을 붙인다.
 */
@Component
public class CommunityPostAssembler {

	private final FindDisplayNameQueryHandler displayNameQueryHandler;
	private final PostHashtagMapper postHashtagMapper;
	private final PostMediaMapper postMediaMapper;
	private final CommunityPostSnapshotCodec snapshotCodec;
	private final PostLikeMapper postLikeMapper;
	private final CommunityCommentMapper communityCommentMapper;
	private final PostRetripMapper postRetripMapper;
	private final MediaFileQueryService mediaFileQueryService;

	public CommunityPostAssembler(
		FindDisplayNameQueryHandler displayNameQueryHandler,
		PostHashtagMapper postHashtagMapper,
		PostMediaMapper postMediaMapper,
		CommunityPostSnapshotCodec snapshotCodec,
		PostLikeMapper postLikeMapper,
		CommunityCommentMapper communityCommentMapper,
		PostRetripMapper postRetripMapper,
		MediaFileQueryService mediaFileQueryService
	) {
		this.displayNameQueryHandler = displayNameQueryHandler;
		this.postHashtagMapper = postHashtagMapper;
		this.postMediaMapper = postMediaMapper;
		this.snapshotCodec = snapshotCodec;
		this.postLikeMapper = postLikeMapper;
		this.communityCommentMapper = communityCommentMapper;
		this.postRetripMapper = postRetripMapper;
		this.mediaFileQueryService = mediaFileQueryService;
	}

	/**
	 * 게시글을 {@link CommunityPostDetail}로 조립한다.
	 *
	 * @param post 게시글 레코드
	 * @param viewerUserId 조회자 (nullable, likedByMe 계산용)
	 * @param rawShareToken raw 공유 토큰 (nullable)
	 * @param includeSnapshot snapshot 포함 여부
	 * @return 상세 DTO
	 */
	public CommunityPostDetail toDetail(
		CommunityPostRecord post,
		UUID viewerUserId,
		String rawShareToken,
		boolean includeSnapshot
	) {
		UserSummary publisher = resolvePublisher(post.publishedByUserId());
		List<String> hashtags = postHashtagMapper.findHashtagNamesByPostId(post.id());
		List<UUID> mediaFileIds = postMediaMapper.findByPostId(post.id()).stream()
			.map(media -> media.mediaFileId())
			.toList();
		List<MediaFile> media = mediaFileQueryService.findByIds(mediaFileIds);
		MediaFile coverMedia = mediaFileQueryService.findById(post.coverMediaFileId()).orElse(null);
		int mediaCount = media.size();
		int likeCount = postLikeMapper.countByPostId(post.id());
		int commentCount = communityCommentMapper.countByPostId(post.id());
		int retripCount = postRetripMapper.countByPostId(post.id());
		boolean likedByMe = viewerUserId != null
			&& postLikeMapper.existsByPostIdAndUserId(post.id(), viewerUserId);

		CommunityPostSnapshot snapshot = includeSnapshot
			? snapshotCodec.decode(post.snapshotJson())
			: new CommunityPostSnapshot(List.of(), List.of(), null);

		return new CommunityPostDetail(
			post.id(),
			post.sourceTripId(),
			publisher,
			coverMedia,
			post.visibility(),
			post.title(),
			post.summary(),
			hashtags,
			likeCount,
			retripCount,
			commentCount,
			mediaCount,
			likedByMe,
			post.moderationStatus(),
			toOffsetDateTime(post.publishedAt()),
			post.snapshotVersion(),
			snapshot,
			media,
			rawShareToken,
			buildShareUrl(post.id(), rawShareToken),
			toOffsetDateTime(post.shareTokenCreatedAt()),
			toOffsetDateTime(post.shareTokenRotatedAt())
		);
	}

	/**
	 * 게시글을 {@link CommunityPostSummary}로 조립한다. snapshot은 포함하지 않는다.
	 *
	 * @param post 게시글 레코드
	 * @param viewerUserId 조회자 (nullable)
	 * @return 요약 DTO
	 */
	public CommunityPostSummary toSummary(CommunityPostRecord post, UUID viewerUserId) {
		UserSummary publisher = resolvePublisher(post.publishedByUserId());
		List<String> hashtags = postHashtagMapper.findHashtagNamesByPostId(post.id());
		int mediaCount = postMediaMapper.countByPostId(post.id());
		MediaFile coverMedia = mediaFileQueryService.findById(post.coverMediaFileId()).orElse(null);
		int likeCount = postLikeMapper.countByPostId(post.id());
		int commentCount = communityCommentMapper.countByPostId(post.id());
		int retripCount = postRetripMapper.countByPostId(post.id());
		boolean likedByMe = viewerUserId != null
			&& postLikeMapper.existsByPostIdAndUserId(post.id(), viewerUserId);

		return new CommunityPostSummary(
			post.id(),
			post.sourceTripId(),
			publisher,
			coverMedia,
			post.visibility(),
			post.title(),
			post.summary(),
			hashtags,
			likeCount, retripCount, commentCount, mediaCount,
			likedByMe,
			post.moderationStatus(),
			toOffsetDateTime(post.publishedAt())
		);
	}

	/**
	 * 댓글 레코드를 {@link CommunityComment} DTO로 조립한다.
	 *
	 * @param record 댓글 레코드
	 * @return 댓글 DTO
	 */
	public CommunityComment toComment(CommunityCommentRecord record) {
		UserSummary author = resolvePublisher(record.authorUserId());
		return new CommunityComment(
			record.id(),
			record.postId(),
			record.parentCommentId(),
			author,
			record.content(),
			record.depth(),
			record.moderationStatus(),
			toOffsetDateTime(record.deletedAt()),
			toOffsetDateTime(record.createdAt())
		);
	}

	private UserSummary resolvePublisher(UUID userId) {
		String displayName = displayNameQueryHandler.handle(new FindDisplayNameQuery(userId));
		URI profileImageUrl = displayNameQueryHandler.findProfileImageUrl(new FindDisplayNameQuery(userId));
		return new UserSummary(userId, displayName, profileImageUrl);
	}

	private URI buildShareUrl(UUID postId, String rawShareToken) {
		if (rawShareToken == null) {
			return null;
		}
		return URI.create("https://soomgil.example.com/community/posts/" + postId + "?share=" + rawShareToken);
	}

	private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
		return instant != null ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
	}
}
