package com.soomgil.media.infrastructure.persistence.mapper;

import com.soomgil.media.infrastructure.persistence.row.MediaFileRow;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** media metadata 저장, 조회, 권한 확인 SQL mapper. */
@Mapper
public interface MediaFileMapper {

	void insert(MediaFileRow row);

	MediaFileRow findById(UUID mediaFileId);

	int markDeleted(@Param("mediaFileId") UUID mediaFileId, @Param("deletedAt") Instant deletedAt,
		@Param("purgeAfter") Instant purgeAfter);


	/** 삭제·숨김되지 않은 공개 커뮤니티 게시물에서 사용하는 미디어인지 확인한다. */
	long countPublishedMedia(@Param("mediaFileId") UUID mediaFileId);

	/** active 여행방 소유자 또는 멤버인지 확인한다. */
	long countAccessibleTrip(@Param("userId") UUID userId, @Param("tripId") UUID tripId);

	List<MediaFileRow> findDueForPurge(@Param("now") Instant now, @Param("limit") int limit);

	int markPurged(@Param("mediaFileId") UUID mediaFileId, @Param("purgedAt") Instant purgedAt);

	int claimUnlinkedForPurge(@Param("mediaFileId") UUID mediaFileId, @Param("now") Instant now);
}
