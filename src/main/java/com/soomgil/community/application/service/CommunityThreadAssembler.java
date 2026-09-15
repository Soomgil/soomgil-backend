package com.soomgil.community.application.service;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadLikeMapper;
import com.soomgil.community.infrastructure.persistence.mapper.ThreadMediaMapper;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.media.application.MediaFileQueryService;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 쓰레드/답글 record를 공개 API 응답 DTO로 조립한다.
 *
 * <p>가장 중요한 책임은 tombstone 처리다. 삭제되거나 숨김 처리된 쓰레드/답글은 식별자와 시각만 남기고
 * 본문을 절대 응답에 넣지 않는다. 작성자 표시 이름은 auth 모듈의 query handler를 통해서만 조회하고
 * auth mapper를 직접 읽지 않는다.
 */
@Component
public class CommunityThreadAssembler {

	private final FindDisplayNameQueryHandler displayNameQueryHandler;
	private final ThreadMediaMapper threadMediaMapper;
	private final ThreadLikeMapper threadLikeMapper;
	private final CommunityThreadReplyMapper replyMapper;
	private final MediaFileQueryService mediaFileQueryService;

	public CommunityThreadAssembler(
		FindDisplayNameQueryHandler displayNameQueryHandler,
		ThreadMediaMapper threadMediaMapper,
		ThreadLikeMapper threadLikeMapper,
		CommunityThreadReplyMapper replyMapper,
		MediaFileQueryService mediaFileQueryService
	) {
		this.displayNameQueryHandler = displayNameQueryHandler;
		this.threadMediaMapper = threadMediaMapper;
		this.threadLikeMapper = threadLikeMapper;
		this.replyMapper = replyMapper;
		this.mediaFileQueryService = mediaFileQueryService;
	}

	/**
	 * 쓰레드 record를 응답 DTO로 조립한다.
	 *
	 * @param record 쓰레드 record
	 * @param viewerUserId 조회자. 비로그인 조회면 null이며 likedByMe/editableByMe가 false가 된다
	 * @return 공개 응답 DTO. 삭제/숨김이면 본문이 null인 tombstone
	 */
	public CommunityThread toThread(CommunityThreadRecord record, UUID viewerUserId) {
		boolean readable = record.isPubliclyReadable();
		List<MediaFile> media = readable
			? mediaFileQueryService.findByIds(threadMediaMapper.findMediaFileIdsByThreadId(record.id()))
			: List.of();

		return new CommunityThread(
			record.id(),
			resolveAuthor(record.authorUserId()),
			readable ? record.content() : null,
			media,
			threadLikeMapper.countByThreadId(record.id()),
			replyMapper.countVisibleByThreadId(record.id()),
			viewerUserId != null && threadLikeMapper.existsByThreadIdAndUserId(record.id(), viewerUserId),
			readable && record.isAuthoredBy(viewerUserId),
			record.moderationStatus(),
			toOffsetDateTime(record.deletedAt()),
			toOffsetDateTime(record.createdAt()),
			toOffsetDateTime(record.updatedAt())
		);
	}

	/**
	 * 답글 record를 응답 DTO로 조립한다. 하위 답글은 포함하지 않는다.
	 *
	 * @param record 답글 record
	 * @param viewerUserId 조회자. 비로그인 조회면 null
	 * @return 공개 응답 DTO. 삭제/숨김이면 본문이 null인 tombstone
	 */
	public CommunityThreadReply toReply(CommunityThreadReplyRecord record, UUID viewerUserId) {
		return toReply(record, viewerUserId, List.of());
	}

	/**
	 * root 답글과 1단계 하위 답글을 하나의 트리로 조립한다.
	 *
	 * <p>중첩은 1단계까지만 허용하므로 하위 답글의 {@code replies}는 항상 빈 목록이다.
	 * 부모가 목록에 없는 하위 답글은 결과에서 제외해 고아 답글이 최상위로 올라오지 않게 한다.
	 *
	 * @param roots root 답글 목록. 조회된 순서를 유지한다
	 * @param children 같은 쓰레드의 1단계 하위 답글 목록
	 * @param viewerUserId 조회자. 비로그인 조회면 null
	 * @return root 답글 순서를 유지한 답글 트리
	 */
	public List<CommunityThreadReply> toReplyTree(
		List<CommunityThreadReplyRecord> roots,
		List<CommunityThreadReplyRecord> children,
		UUID viewerUserId
	) {
		Map<UUID, List<CommunityThreadReply>> childrenByParent = new LinkedHashMap<>();
		for (CommunityThreadReplyRecord child : children) {
			childrenByParent
				.computeIfAbsent(child.parentReplyId(), key -> new ArrayList<>())
				.add(toReply(child, viewerUserId, List.of()));
		}

		List<CommunityThreadReply> result = new ArrayList<>();
		for (CommunityThreadReplyRecord root : roots) {
			result.add(toReply(root, viewerUserId, childrenByParent.getOrDefault(root.id(), List.of())));
		}
		return List.copyOf(result);
	}

	private CommunityThreadReply toReply(
		CommunityThreadReplyRecord record,
		UUID viewerUserId,
		List<CommunityThreadReply> replies
	) {
		boolean readable = record.isPubliclyReadable();
		return new CommunityThreadReply(
			record.id(),
			record.threadId(),
			record.parentReplyId(),
			resolveAuthor(record.authorUserId()),
			readable ? record.content() : null,
			record.depth(),
			readable && record.isAuthoredBy(viewerUserId),
			record.moderationStatus(),
			toOffsetDateTime(record.deletedAt()),
			toOffsetDateTime(record.createdAt()),
			toOffsetDateTime(record.updatedAt()),
			replies
		);
	}

	private UserSummary resolveAuthor(UUID userId) {
		String displayName = displayNameQueryHandler.handle(new FindDisplayNameQuery(userId));
		URI profileImageUrl = displayNameQueryHandler.findProfileImageUrl(new FindDisplayNameQuery(userId));
		return new UserSummary(userId, displayName, profileImageUrl);
	}

	private OffsetDateTime toOffsetDateTime(Instant instant) {
		return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}
