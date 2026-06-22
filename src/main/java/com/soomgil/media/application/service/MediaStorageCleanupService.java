package com.soomgil.media.application.service;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.storage.ObjectStorageGateway;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.infrastructure.persistence.mapper.MediaFileMapper;
import com.soomgil.media.infrastructure.persistence.mapper.MediaUploadIntentMapper;
import com.soomgil.media.infrastructure.persistence.row.MediaFileRow;
import com.soomgil.media.infrastructure.persistence.row.MediaUploadIntentRow;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** 만료된 업로드와 삭제 보존 기간이 지난 object를 storage에서 정리한다. */
@Service
public class MediaStorageCleanupService {

	private static final Logger log = LoggerFactory.getLogger(MediaStorageCleanupService.class);
	private static final int BATCH_SIZE = 100;
	private final MediaUploadIntentMapper uploadIntentMapper;
	private final MediaFileMapper mediaFileMapper;
	private final ObjectStorageGateway storage;
	private final TimeProvider timeProvider;

	public MediaStorageCleanupService(
		MediaUploadIntentMapper uploadIntentMapper,
		MediaFileMapper mediaFileMapper,
		ObjectStorageGateway storage,
		TimeProvider timeProvider
	) {
		this.uploadIntentMapper = uploadIntentMapper;
		this.mediaFileMapper = mediaFileMapper;
		this.storage = storage;
		this.timeProvider = timeProvider;
	}

	@Scheduled(fixedDelayString = "${soomgil.storage.cleanup-delay-ms:3600000}")
	public void cleanup() {
		OffsetDateTime now = OffsetDateTime.ofInstant(timeProvider.now(), ZoneOffset.UTC);
		for (MediaUploadIntentRow intent : uploadIntentMapper.findExpiredPending(now, BATCH_SIZE)) {
			runSafely(intent.objectKey(), () -> deleteIntentObject(intent));
		}
		for (MediaUploadIntentRow intent : uploadIntentMapper.findExpiredCompletedUnlinked(now, BATCH_SIZE)) {
			runSafely(intent.objectKey(), () -> {
				if (mediaFileMapper.claimUnlinkedForPurge(intent.mediaFileId(), now.toInstant()) != 1) return;
				storage.delete(new StorageObjectKey(intent.objectKey()));
				mediaFileMapper.markPurged(intent.mediaFileId(), now.toInstant());
				uploadIntentMapper.markPurged(intent.id());
			});
		}
		Instant instant = now.toInstant();
		for (MediaFileRow media : mediaFileMapper.findDueForPurge(instant, BATCH_SIZE)) {
			runSafely(media.objectKey(), () -> {
				storage.delete(new StorageObjectKey(media.objectKey()));
				mediaFileMapper.markPurged(media.id(), instant);
				uploadIntentMapper.markPurgedByMediaFileId(media.id());
			});
		}
	}

	private void runSafely(String objectKey, Runnable cleanupAction) {
		try {
			cleanupAction.run();
		}
		catch (RuntimeException exception) {
			log.warn("Media storage cleanup failed for objectKey={}; it will be retried.", objectKey, exception);
		}
	}

	private void deleteIntentObject(MediaUploadIntentRow intent) {
		if ("PENDING".equals(intent.status()) && uploadIntentMapper.claimPendingForPurge(intent.id()) != 1) return;
		storage.delete(new StorageObjectKey(intent.objectKey()));
		uploadIntentMapper.markPurged(intent.id());
	}
}
