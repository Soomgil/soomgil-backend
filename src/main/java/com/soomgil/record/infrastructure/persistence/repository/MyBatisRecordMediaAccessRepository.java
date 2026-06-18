package com.soomgil.record.infrastructure.persistence.repository;

import com.soomgil.record.application.port.RecordMediaAccessRepository;
import com.soomgil.record.infrastructure.persistence.mapper.RecordMediaAccessMapper;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 여행 기록 미디어 접근 권한 repository.
 */
@Repository
public class MyBatisRecordMediaAccessRepository implements RecordMediaAccessRepository {

	private final RecordMediaAccessMapper mapper;

	public MyBatisRecordMediaAccessRepository(RecordMediaAccessMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public boolean areLinkable(UUID recordId, UUID userId, List<UUID> mediaFileIds) {
		List<UUID> distinctIds = mediaFileIds.stream().distinct().toList();
		return distinctIds.isEmpty() || mapper.countLinkable(recordId, userId, distinctIds) == distinctIds.size();
	}
}
