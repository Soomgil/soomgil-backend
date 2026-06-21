package com.soomgil.media.application.port;

import com.soomgil.media.domain.model.MediaFileMetadata;
import java.time.Instant;
import java.util.UUID;

/** media metadata 저장과 soft delete를 수행하는 persistence 계약. */
public interface MediaFileRepository {

	void save(MediaFileMetadata mediaFile);

	MediaFileMetadata findById(UUID mediaFileId);

	boolean markDeleted(UUID mediaFileId, Instant deletedAt, Instant purgeAfter);
}
