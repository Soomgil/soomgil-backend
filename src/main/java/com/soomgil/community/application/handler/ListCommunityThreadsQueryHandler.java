package com.soomgil.community.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.PagedCommunityThread;
import com.soomgil.community.application.query.ListCommunityThreadsQuery;
import com.soomgil.community.application.service.CommunityThreadAssembler;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityThreadMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListCommunityThreadsQuery}를 처리해 공개 피드를 조회한다.
 *
 * <p>공개 API이므로 인증을 요구하지 않는다. 삭제/숨김 쓰레드는 mapper 단계에서 제외되며 정렬은
 * {@code createdAt desc, id desc}로 고정한다. {@code page}는 0부터 시작하고 {@code size}는
 * 1~100 범위로 보정한다.
 */
@Component
@Transactional(readOnly = true)
public class ListCommunityThreadsQueryHandler
	implements QueryHandler<ListCommunityThreadsQuery, PagedCommunityThread> {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final List<String> FIXED_SORT = List.of("createdAt,desc", "id,desc");

	private final CommunityThreadMapper threadMapper;
	private final CommunityThreadAssembler assembler;

	public ListCommunityThreadsQueryHandler(
		CommunityThreadMapper threadMapper,
		CommunityThreadAssembler assembler
	) {
		this.threadMapper = threadMapper;
		this.assembler = assembler;
	}

	@Override
	public PagedCommunityThread handle(ListCommunityThreadsQuery query) {
		int page = Math.max(query.page(), 0);
		int size = normalizeSize(query.size());
		int offset = page * size;

		List<CommunityThread> items = threadMapper
			.findFeed(query.authorId(), normalizeQuery(query.query()), offset, size)
			.stream()
			.map(record -> assembler.toThread(record, query.viewerUserId()))
			.toList();
		long totalElements = threadMapper.countFeed(query.authorId(), normalizeQuery(query.query()));
		int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

		return new PagedCommunityThread(
			items,
			new PageMeta(page, size, totalElements, totalPages, FIXED_SORT)
		);
	}

	private int normalizeSize(int size) {
		if (size < 1) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}

	private String normalizeQuery(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.strip();
		return normalized.isEmpty() ? null : normalized;
	}
}
