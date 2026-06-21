package com.soomgil.community.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.application.query.GetCommunityPostQuery;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.application.service.ShareTokenService;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.global.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글 상세를 조회한다.
 *
 * <p>접근 권한 규칙:
 * <ul>
 *   <li>삭제된 게시글 — 발행자 본인만 조회 가능, 그 외에는 POST_NOT_FOUND</li>
 *   <li>PUBLIC + VISIBLE — 누구나 조회 가능</li>
 *   <li>UNLISTED 또는 모더레이션 비노출 — 발행자 본인, 또는 유효한 share token 소지자만</li>
 * </ul>
 * <p>UNLISTED 게시글에 token 없이 접근하면 존재를 숨기기 위해 POST_NOT_FOUND,
 * token이 제공됐지만 invalid하면 INVALID_SHARE_TOKEN을 반환한다.
 */
@Component
@Transactional(readOnly = true)
public class GetCommunityPostQueryHandler
	implements QueryHandler<GetCommunityPostQuery, CommunityPostDetail> {

	private final CommunityPostMapper postMapper;
	private final ShareTokenService shareTokenService;
	private final CommunityPostAssembler assembler;

	public GetCommunityPostQueryHandler(
		CommunityPostMapper postMapper,
		ShareTokenService shareTokenService,
		CommunityPostAssembler assembler
	) {
		this.postMapper = postMapper;
		this.shareTokenService = shareTokenService;
		this.assembler = assembler;
	}

	@Override
	public CommunityPostDetail handle(GetCommunityPostQuery query) {
		CommunityPostRecord post = postMapper.findById(query.postId())
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));

		UUID viewer = query.viewerUserId();
		boolean isPublisher = post.isPublishedBy(viewer);

		if (post.isDeleted() && !isPublisher) {
			throw new CommunityException(ErrorCode.POST_NOT_FOUND);
		}

		if (!isPublisher && !post.isPubliclyVisible()) {
			if (!post.hasShareToken()) {
				throw new CommunityException(ErrorCode.POST_NOT_FOUND);
			}
			if (query.shareToken() == null || query.shareToken().isBlank()) {
				throw new CommunityException(ErrorCode.POST_NOT_FOUND);
			}
			String providedHash = shareTokenService.hash(query.shareToken());
			if (!providedHash.equals(post.shareTokenHash())) {
				throw new CommunityException(ErrorCode.INVALID_SHARE_TOKEN);
			}
		}

		return assembler.toDetail(post, viewer, null, true);
	}
}
