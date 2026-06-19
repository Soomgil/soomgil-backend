package com.soomgil.record.infrastructure.persistence.mapper;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 여행 기록 미디어 접근 권한 SQL mapper.
 */
@Mapper
public interface RecordMediaAccessMapper {

	long countLinkable(
		@Param("recordId") UUID recordId,
		@Param("userId") UUID userId,
		@Param("mediaFileIds") List<UUID> mediaFileIds
	);
}
