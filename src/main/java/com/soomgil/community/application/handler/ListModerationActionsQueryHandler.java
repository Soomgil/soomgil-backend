package com.soomgil.community.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.PagedModerationAction;
import com.soomgil.community.application.query.ListModerationActionsQuery;
import com.soomgil.community.application.service.CommunityReportAssembler;
import com.soomgil.community.domain.model.ModerationActionRecord;
import com.soomgil.community.infrastructure.persistence.mapper.ModerationActionMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모더레이션 조치 이력을 조회한다 (모더레이터 전용).
 *
 * <p>생성일 내림차순으로 정렬하며, page/size 페이지네이션을 지원한다.
 */
@Component
@Transactional(readOnly = true)
public class ListModerationActionsQueryHandler
	implements QueryHandler<ListModerationActionsQuery, PagedModerationAction> {

	private static final int MAX_SIZE = 100;
	private static final int DEFAULT_SIZE = 20;

	private final ModerationActionMapper actionMapper;
	private final CommunityReportAssembler assembler;

	public ListModerationActionsQueryHandler(
		ModerationActionMapper actionMapper,
		CommunityReportAssembler assembler
	) {
		this.actionMapper = actionMapper;
		this.assembler = assembler;
	}

	@Override
	public PagedModerationAction handle(ListModerationActionsQuery query) {
		int size = sanitizeSize(query.size());
		int page = Math.max(0, query.page());
		int offset = page * size;

		List<ModerationActionRecord> rows = actionMapper.findAll(offset, size);
		long total = actionMapper.countAll();

		List<ModerationAction> items = rows.stream()
			.map(assembler::toAction)
			.toList();

		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
		PageMeta pageMeta = new PageMeta(page, size, total, totalPages, List.of());

		return new PagedModerationAction(items, pageMeta);
	}

	private int sanitizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}
}
