package com.soomgil.media.infrastructure.persistence.mapper;

import com.soomgil.media.infrastructure.persistence.row.MediaFileRow;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** media metadata 저장, 조회, 권한 확인 SQL mapper. */
@Mapper
public interface MediaFileMapper {

	void insert(MediaFileRow row);

	MediaFileRow findById(UUID mediaFileId);

	int markDeleted(@Param("mediaFileId") UUID mediaFileId, @Param("deletedAt") Instant deletedAt,
		@Param("purgeAfter") Instant purgeAfter);

	long countAccessibleTripRecord(@Param("userId") UUID userId, @Param("recordId") UUID recordId);
}
