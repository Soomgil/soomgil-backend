package com.soomgil.community.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.PagedCommunityThreadReply;
import com.soomgil.community.application.query.ListCommunityThreadRepliesQuery;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityThreadRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadReplyMapper;
import com.soomgil.global.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListCommunityThreadRepliesQuery}를 처리해 답글 트리를 조회한다.
 *
 * <p>page 단위는 root 답글이며 각 root 답글의 1단계 하위 답글은 함께 반환되지만 page 계산에는
 * 포함되지 않는다. 정렬은 {@code createdAt asc, id asc}로 고정한다.
 */
@Component
@Transactional(readOnly = true)
public class ListCommunityThreadRepliesQueryHandler
	implements QueryHandler<ListCommunityThreadRepliesQuery, PagedCommunityThreadReply> {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final List<String> FIXED_SORT = List.of("createdAt,asc", "id,asc");

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadReplyMapper replyMapper;
	private final CommunityThreadAssembler assembler;

	public ListCommunityThreadRepliesQueryHandler(
		CommunityThreadMapper threadMapper,
		CommunityThreadReplyMapper replyMapper,
		CommunityThreadAssembler assembler
	) {
		this.threadMapper = threadMapper;
		this.replyMapper = replyMapper;
		this.assembler = assembler;
	}

	@Override
	public PagedCommunityThreadReply handle(ListCommunityThreadRepliesQuery query) {
		threadMapper.findById(query.threadId())
			.filter(CommunityThreadRecord::isPubliclyReadable)
			.orElseThrow(() -> new CommunityException(ErrorCode.THREAD_NOT_FOUND));

		int page = Math.max(query.page(), 0);
		int size = normalizeSize(query.size());
		int offset = page * size;

		long totalElements = replyMapper.countRootReplies(query.threadId());
		int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

		return new PagedCommunityThreadReply(
			assembler.toReplyTree(
				replyMapper.findRootReplies(query.threadId(), offset, size),
				replyMapper.findChildReplies(query.threadId()),
				query.viewerUserId()
			),
			new PageMeta(page, size, totalElements, totalPages, FIXED_SORT)
		);
	}

	private int normalizeSize(int size) {
		if (size < 1) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
