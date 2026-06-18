package com.soomgil.auth.application.handler;

import com.soomgil.auth.api.dto.PagedUserSession;
import com.soomgil.auth.api.dto.UserSession;
import com.soomgil.auth.application.query.ListSessionsQuery;
import com.soomgil.auth.infrastructure.persistence.UserSessionMapper;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 세션 목록을 조회한다.
 */
@Component
@Transactional(readOnly = true)
public class ListSessionsQueryHandler implements QueryHandler<ListSessionsQuery, PagedUserSession> {

	private final UserSessionMapper userSessionMapper;

	public ListSessionsQueryHandler(UserSessionMapper userSessionMapper) {
		this.userSessionMapper = userSessionMapper;
	}

	@Override
	public PagedUserSession handle(ListSessionsQuery query) {
		int offset = query.page() * query.size();
		List<UserSession> items = userSessionMapper.findByUserId(query.userId(), offset, query.size());
		long total = userSessionMapper.countByUserId(query.userId());
		int totalPages = query.size() > 0 ? (int) ((total + query.size() - 1) / query.size()) : 0;

		PageMeta page = new PageMeta(query.page(), query.size(), total, totalPages, null);
		return new PagedUserSession(items, page);
	}
}
