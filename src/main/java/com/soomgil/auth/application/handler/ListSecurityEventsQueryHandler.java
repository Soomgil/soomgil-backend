package com.soomgil.auth.application.handler;

import com.soomgil.auth.api.dto.PagedSecurityEvent;
import com.soomgil.auth.api.dto.SecurityEvent;
import com.soomgil.auth.application.query.ListSecurityEventsQuery;
import com.soomgil.auth.infrastructure.persistence.SecurityEventMapper;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 보안 이벤트 목록을 조회한다.
 */
@Component
@Transactional(readOnly = true)
public class ListSecurityEventsQueryHandler implements QueryHandler<ListSecurityEventsQuery, PagedSecurityEvent> {

	private final SecurityEventMapper securityEventMapper;

	public ListSecurityEventsQueryHandler(SecurityEventMapper securityEventMapper) {
		this.securityEventMapper = securityEventMapper;
	}

	@Override
	public PagedSecurityEvent handle(ListSecurityEventsQuery query) {
		int offset = query.page() * query.size();
		List<SecurityEvent> items = securityEventMapper.findByUserId(query.userId(), offset, query.size())
			.stream()
			.map(SecurityEventMapper.SecurityEventRow::toDto)
			.toList();
		long total = securityEventMapper.countByUserId(query.userId());
		int totalPages = query.size() > 0 ? (int) ((total + query.size() - 1) / query.size()) : 0;

		PageMeta page = new PageMeta(query.page(), query.size(), total, totalPages, null);
		return new PagedSecurityEvent(items, page);
	}
}
