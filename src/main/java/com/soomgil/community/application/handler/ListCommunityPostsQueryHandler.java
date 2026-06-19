package com.soomgil.community.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.CommunityPostSummary;
import com.soomgil.community.api.dto.PagedCommunityPostSummary;
import com.soomgil.community.application.query.ListCommunityPostsQuery;
import com.soomgil.community.application.service.CommunityPostAssembler;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 게시글 목록을 조회한다.
 *
 * <p>{@code publisherUserId}가 지정되면 해당 사용자가 발행한 게시글을,
 * 지정되지 않으면 공개 feed(visibility=PUBLIC, moderation=VISIBLE)를 반환한다.
 * MVP에서는 해시태그/검색어 필터를 지원하지 않는다.
 */
@Component
@Transactional(readOnly = true)
public class ListCommunityPostsQueryHandler
	implements QueryHandler<ListCommunityPostsQuery, PagedCommunityPostSummary> {

	private static final int MAX_SIZE = 100;
	private static final int DEFAULT_SIZE = 20;

	private final CommunityPostMapper postMapper;
	private final CommunityPostAssembler assembler;

	public ListCommunityPostsQueryHandler(
		CommunityPostMapper postMapper,
		CommunityPostAssembler assembler
	) {
		this.postMapper = postMapper;
		this.assembler = assembler;
	}

	@Override
	public PagedCommunityPostSummary handle(ListCommunityPostsQuery query) {
		int size = sanitizeSize(query.size());
		int page = Math.max(0, query.page());
		int offset = page * size;

		List<CommunityPostRecord> rows;
		long total;
		if (query.publisherUserId() != null) {
			rows = postMapper.findByPublisher(query.publisherUserId(), offset, size);
			total = postMapper.countByPublisher(query.publisherUserId());
		} else {
			rows = postMapper.findPublicFeed(offset, size);
			total = postMapper.countPublicFeed();
		}

		List<CommunityPostSummary> items = rows.stream()
			.map(post -> assembler.toSummary(post, query.viewerUserId()))
			.toList();

		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
		PageMeta pageMeta = new PageMeta(page, size, total, totalPages, List.of());

		return new PagedCommunityPostSummary(items, pageMeta);
	}

	private int sanitizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
