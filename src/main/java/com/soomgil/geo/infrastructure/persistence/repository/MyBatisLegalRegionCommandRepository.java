package com.soomgil.geo.infrastructure.persistence.repository;

import com.soomgil.geo.application.port.LegalRegionCommandRepository;
import com.soomgil.geo.application.port.LegalRegionSyncLog;
import com.soomgil.geo.application.port.LegalRegionUpsert;
import com.soomgil.geo.infrastructure.persistence.mapper.LegalRegionCommandMapper;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 법정동 지역 쓰기 repository.
 *
 * <p>CSV 동기화 handler가 계산한 row 모델을 DBML의 {@code geo.legal_regions}와
 * {@code geo.legal_region_sync_logs}에 저장한다.
 */
@Repository
public class MyBatisLegalRegionCommandRepository implements LegalRegionCommandRepository {

	private final LegalRegionCommandMapper mapper;

	public MyBatisLegalRegionCommandRepository(LegalRegionCommandMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public boolean existsLegalRegion(String code) {
		return mapper.existsLegalRegion(code);
	}

	@Override
	public void upsertLegalRegion(LegalRegionUpsert region) {
		mapper.upsertLegalRegion(region);
	}

	@Override
	public void saveSyncLog(LegalRegionSyncLog log) {
		mapper.insertSyncLog(log);
	}
}
