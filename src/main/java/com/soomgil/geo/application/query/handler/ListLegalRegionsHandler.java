package com.soomgil.geo.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.geo.application.port.LegalRegionPage;
import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.application.query.dto.ListLegalRegionsQuery;
import com.soomgil.geo.application.query.dto.PagedLegalRegionView;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListLegalRegionsQuery}를 처리해 법정동 지역 목록을 조회한다.
 */
@Component
public class ListLegalRegionsHandler implements QueryHandler<ListLegalRegionsQuery, PagedLegalRegionView> {

	private static final int MAX_PAGE_SIZE = 100;

	private final LegalRegionQueryRepository repository;

	public ListLegalRegionsHandler(LegalRegionQueryRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public PagedLegalRegionView handle(ListLegalRegionsQuery query) {
		if (query.page() < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Page must be greater than or equal to 0.");
		}
		if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Size must be between 1 and 100.");
		}
		if (query.parentCode() != null && query.parentCode().length() != 10) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Parent code must be 10 characters.");
		}

		LegalRegionPage result = repository.findLegalRegions(
			query.query(),
			query.level(),
			query.parentCode(),
			query.isActive(),
			query.page(),
			query.size()
		);
		List<LegalRegionView> items = result.items()
			.stream()
			.map(region -> new LegalRegionView(
				region.code(),
				region.name(),
				region.fullName(),
				region.level(),
				region.parentCode(),
				region.active()
			))
			.toList();
		int totalPages = (int) Math.ceil((double) result.totalElements() / query.size());
		return new PagedLegalRegionView(
			items,
			query.page(),
			query.size(),
			result.totalElements(),
			totalPages,
			query.sort()
		);
	}
}
