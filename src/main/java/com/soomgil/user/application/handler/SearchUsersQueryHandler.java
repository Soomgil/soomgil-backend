package com.soomgil.user.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.api.dto.UserSummary;
import com.soomgil.user.application.query.SearchUsersQuery;
import com.soomgil.user.domain.model.UserSummaryRecord;
import com.soomgil.user.infrastructure.persistence.UserSearchMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 검색({@code GET /users})을 처리한다.
 *
 * <p>검색 대상은 {@code PUBLIC} 프로필만. {@code PRIVATE} 프로필은 노출하지 않는다.
 * 결과는 display name 오름차순 정렬한다.
 */
@Component
@Transactional(readOnly = true)
public class SearchUsersQueryHandler implements QueryHandler<SearchUsersQuery, PagedUserSummary> {

	private static final int MAX_SIZE = 100;
	private static final int DEFAULT_SIZE = 20;

	private final UserSearchMapper userSearchMapper;

	public SearchUsersQueryHandler(UserSearchMapper userSearchMapper) {
		this.userSearchMapper = userSearchMapper;
	}

	@Override
	public PagedUserSummary handle(SearchUsersQuery query) {
		int size = sanitizeSize(query.size());
		int page = Math.max(0, query.page());
		int offset = page * size;

		String q = (query.query() == null || query.query().isBlank()) ? null : query.query();

		List<UserSummaryRecord> rows = userSearchMapper.search(q, size, offset);
		long total = userSearchMapper.count(q);

		List<UserSummary> items = rows.stream()
			.map(r -> new UserSummary(r.userId(), r.displayName(), r.profileImageUrl()))
			.toList();

		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
		PageMeta pageMeta = new PageMeta(page, size, total, totalPages, List.of());

		return new PagedUserSummary(items, pageMeta);
	}

	private int sanitizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
