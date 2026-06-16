package com.soomgil.geo.infrastructure.persistence.repository;

import com.soomgil.geo.application.port.LegalRegionPage;
import com.soomgil.geo.application.port.LegalRegionQueryRepository;
import com.soomgil.geo.application.port.LegalRegionReadModel;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.geo.infrastructure.persistence.mapper.LegalRegionQueryMapper;
import com.soomgil.geo.infrastructure.persistence.row.LegalRegionRow;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 법정동 지역 읽기 repository.
 */
@Repository
public class MyBatisLegalRegionQueryRepository implements LegalRegionQueryRepository {

	private final LegalRegionQueryMapper mapper;

	public MyBatisLegalRegionQueryRepository(LegalRegionQueryMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public LegalRegionPage findLegalRegions(
		String query,
		LegalRegionLevel level,
		String parentCode,
		Boolean isActive,
		int page,
		int size
	) {
		String levelValue = level == null ? null : level.name();
		int offset = page * size;
		List<LegalRegionReadModel> items = mapper.findLegalRegions(query, levelValue, parentCode, isActive, size, offset)
			.stream()
			.map(this::toReadModel)
			.toList();
		long totalElements = mapper.countLegalRegions(query, levelValue, parentCode, isActive);
		return new LegalRegionPage(items, totalElements);
	}

	private LegalRegionReadModel toReadModel(LegalRegionRow row) {
		return new LegalRegionReadModel(
			row.code(),
			row.name(),
			row.fullName(),
			LegalRegionLevel.valueOf(row.level()),
			row.parentCode(),
			Boolean.TRUE.equals(row.isActive())
		);
	}
}
