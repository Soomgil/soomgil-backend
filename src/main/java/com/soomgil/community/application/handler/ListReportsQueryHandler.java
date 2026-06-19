package com.soomgil.community.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.PagedContentReport;
import com.soomgil.community.application.query.ListReportsQuery;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.domain.model.ContentReportRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ContentReportMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 목록을 조회한다 (모더레이터 전용).
 *
 * <p>{@code status} 필터와 page/size 페이지네이션을 지원한다.
 * 생성일 내림차순으로 정렬한다.
 */
@Component
@Transactional(readOnly = true)
public class ListReportsQueryHandler
	implements QueryHandler<ListReportsQuery, PagedContentReport> {

	private static final int MAX_SIZE = 100;
	private static final int DEFAULT_SIZE = 20;

	private final ContentReportMapper reportMapper;
	private final CommunityReportAssembler assembler;

	public ListReportsQueryHandler(
		ContentReportMapper reportMapper,
		CommunityReportAssembler assembler
	) {
		this.reportMapper = reportMapper;
		this.assembler = assembler;
	}

	@Override
	public PagedContentReport handle(ListReportsQuery query) {
		int size = sanitizeSize(query.size());
		int page = Math.max(0, query.page());
		int offset = page * size;

		List<ContentReportRecord> rows = reportMapper.findByStatus(query.status(), offset, size);
		long total = reportMapper.countByStatus(query.status());

		List<ContentReport> items = rows.stream()
			.map(assembler::toReport)
			.toList();

		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
		PageMeta pageMeta = new PageMeta(page, size, total, totalPages, List.of());

		return new PagedContentReport(items, pageMeta);
	}

	private int sanitizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
