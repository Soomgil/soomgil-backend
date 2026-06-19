package com.soomgil.community.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.CommunityComment;
import com.soomgil.community.api.dto.PagedCommunityComment;
import com.soomgil.community.application.query.ListCommentsQuery;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.domain.model.CommunityCommentRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityCommentMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글의 댓글 목록을 조회한다.
 *
 * <p>활성 댓글(VISIBLE, 미삭제)만 반환하며, created_at 오름차순으로 정렬한다.
 */
@Component
@Transactional(readOnly = true)
public class ListCommentsQueryHandler
	implements QueryHandler<ListCommentsQuery, PagedCommunityComment> {

	private static final int MAX_SIZE = 100;
	private static final int DEFAULT_SIZE = 20;

	private final CommunityCommentMapper commentMapper;
	private final CommunityPostAssembler assembler;

	public ListCommentsQueryHandler(
		CommunityCommentMapper commentMapper,
		CommunityPostAssembler assembler
	) {
		this.commentMapper = commentMapper;
		this.assembler = assembler;
	}

	@Override
	public PagedCommunityComment handle(ListCommentsQuery query) {
		int size = sanitizeSize(query.size());
		int page = Math.max(0, query.page());
		int offset = page * size;

		List<CommunityCommentRecord> rows = commentMapper.findByPostId(query.postId(), offset, size);
		long total = commentMapper.countByPostId(query.postId());

		List<CommunityComment> items = rows.stream()
			.map(assembler::toComment)
			.toList();

		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
		PageMeta pageMeta = new PageMeta(page, size, total, totalPages, List.of());

		return new PagedCommunityComment(items, pageMeta);
	}

	private int sanitizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
