package com.soomgil.media.infrastructure.persistence.repository;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.domain.model.MediaFileMetadata;
import com.soomgil.media.infrastructure.persistence.mapper.MediaFileMapper;
import com.soomgil.media.infrastructure.persistence.row.MediaFileRow;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** MyBatis 기반 media metadata repository. */
@Repository
public class MyBatisMediaFileRepository implements MediaFileRepository {

	private final MediaFileMapper mapper;

	public MyBatisMediaFileRepository(MediaFileMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void save(MediaFileMetadata mediaFile) {
		try {
			mapper.insert(toRow(mediaFile));
		}
		catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.CONFLICT, "Media object was already registered.");
		}
	}

	@Override
	public MediaFileMetadata findById(UUID mediaFileId) {
		MediaFileRow row = mapper.findById(mediaFileId);
		return row == null ? null : toDomain(row);
	}

	@Override
	public boolean markDeleted(UUID mediaFileId, Instant deletedAt, Instant purgeAfter) {
		return mapper.markDeleted(mediaFileId, deletedAt, purgeAfter) == 1;
	}

	private MediaFileRow toRow(MediaFileMetadata value) {
		return new MediaFileRow(
			value.id(), value.ownerUserId(), value.storageProvider(), value.bucket(), value.objectKey().value(),
			value.publicUrl() == null ? null : value.publicUrl().toString(), value.mimeType(), value.byteSize(),
			value.width(), value.height(), value.linkedResourceType(), value.linkedResourceId(), value.status(),
			value.createdAt(), value.deletedAt(), value.purgeAfterAt()
		);
	}

	private MediaFileMetadata toDomain(MediaFileRow row) {
		return new MediaFileMetadata(
			row.id(), row.ownerUserId(), row.storageProvider(), row.bucket(), new StorageObjectKey(row.objectKey()),
			row.publicUrl() == null ? null : URI.create(row.publicUrl()), row.mimeType(), row.byteSize(), row.width(),
			row.height(), row.linkedResourceType(), row.linkedResourceId(), row.status(), row.createdAt(),
			row.deletedAt(), row.purgeAfterAt()
		);
	}
}
