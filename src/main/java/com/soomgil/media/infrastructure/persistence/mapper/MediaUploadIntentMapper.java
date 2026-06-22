package com.soomgil.media.infrastructure.persistence.mapper;

import com.soomgil.media.infrastructure.persistence.row.MediaUploadIntentRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 업로드 intent 생성, 완료와 만료 정리 SQL mapper. */
@Mapper
public interface MediaUploadIntentMapper {

	void insert(MediaUploadIntentRow row);

	MediaUploadIntentRow findPendingOwned(@Param("ownerUserId") UUID ownerUserId, @Param("objectKey") String objectKey);

	int markCompleted(@Param("id") UUID id, @Param("mediaFileId") UUID mediaFileId, @Param("completedAt") OffsetDateTime completedAt);

	List<MediaUploadIntentRow> findExpiredPending(@Param("now") OffsetDateTime now, @Param("limit") int limit);

	int claimPendingForPurge(@Param("id") UUID id);

	List<MediaUploadIntentRow> findExpiredCompletedUnlinked(@Param("now") OffsetDateTime now, @Param("limit") int limit);

	int markPurged(@Param("id") UUID id);

	int markPurgedByMediaFileId(@Param("mediaFileId") UUID mediaFileId);
}
