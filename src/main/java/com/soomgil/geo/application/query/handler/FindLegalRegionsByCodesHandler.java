package com.soomgil.geo.application.query.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.query.dto.FindLegalRegionsByCodesQuery;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FindLegalRegionsByCodesQuery}를 처리해 코드 목록에 해당하는 법정동 지역을 조회한다.
 *
 * <p>다른 모듈이 geo 테이블을 직접 읽지 않고 지역 이름을 얻는 공개 진입점이다.
 */
@Component
public class FindLegalRegionsByCodesHandler
	implements QueryHandler<FindLegalRegionsByCodesQuery, List<LegalRegionView>> {
	private final LegalRegionQueryRepository repository;

	public FindLegalRegionsByCodesHandler(LegalRegionQueryRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Override
	@Transactional(readOnly = true)
	public List<LegalRegionView> handle(FindLegalRegionsByCodesQuery query) {
		if (query.codes().isEmpty()) {
			return List.of();
		}
		for (String code : query.codes()) {
			if (code == null || !code.matches("\\d{10}")) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal region code must be 10 digits.");
			}
		}
		return repository.findLegalRegionsByCodes(query.codes().stream().distinct().toList())
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
	}
}
