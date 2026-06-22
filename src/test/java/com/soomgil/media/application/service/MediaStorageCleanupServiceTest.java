package com.soomgil.media.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MediaStorageCleanupServiceTest {

	private static final Instant NOW = Instant.parse("2026-06-22T00:00:00Z");

	@Test
	void deletesExpiredPendingUploadsAndDueDeletedMedia() {
		MediaUploadIntentMapper intents = mock(MediaUploadIntentMapper.class);
		MediaFileMapper media = mock(MediaFileMapper.class);
		ObjectStorageGateway storage = mock(ObjectStorageGateway.class);
		TimeProvider time = () -> NOW;
		UUID intentId = UUID.randomUUID();
		UUID mediaId = UUID.randomUUID();
		OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
		when(intents.findExpiredPending(now, 100)).thenReturn(List.of(new MediaUploadIntentRow(
			intentId, UUID.randomUUID(), "media/user/trip-record/pending.jpg", "PENDING", null,
			now.minusMinutes(1), now.minusDays(1), null
		)));
		when(intents.findExpiredCompletedUnlinked(now, 100)).thenReturn(List.of());
		when(intents.claimPendingForPurge(intentId)).thenReturn(1);
		when(media.findDueForPurge(NOW, 100)).thenReturn(List.of(new MediaFileRow(
			mediaId, UUID.randomUUID(), "S3_COMPATIBLE", "bucket", "media/user/trip-record/deleted.jpg",
			null, "image/jpeg", 10L, 1, 1, null, null, "DELETED", now.minusDays(8), now.minusDays(7), now
		)));

		new MediaStorageCleanupService(intents, media, storage, time).cleanup();

		verify(storage).delete(new StorageObjectKey("media/user/trip-record/pending.jpg"));
		verify(storage).delete(new StorageObjectKey("media/user/trip-record/deleted.jpg"));
		verify(intents).markPurged(intentId);
		verify(media).markPurged(mediaId, NOW);
		verify(intents).markPurgedByMediaFileId(mediaId);
	}

	@Test
	void deletesExpiredCompletedUploadOnlyAfterClaimingItIsStillUnlinked() {
		MediaUploadIntentMapper intents = mock(MediaUploadIntentMapper.class);
		MediaFileMapper media = mock(MediaFileMapper.class);
		ObjectStorageGateway storage = mock(ObjectStorageGateway.class);
		OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
		UUID intentId = UUID.randomUUID();
		UUID mediaId = UUID.randomUUID();
		MediaUploadIntentRow intent = new MediaUploadIntentRow(
			intentId, UUID.randomUUID(), "media/user/trip-record/orphan.jpg", "COMPLETED", mediaId,
			now.minusMinutes(1), now.minusDays(1), now.minusDays(1)
		);
		when(intents.findExpiredPending(now, 100)).thenReturn(List.of());
		when(intents.findExpiredCompletedUnlinked(now, 100)).thenReturn(List.of(intent));
		when(media.claimUnlinkedForPurge(mediaId, NOW)).thenReturn(1);
		when(media.findDueForPurge(NOW, 100)).thenReturn(List.of());

		new MediaStorageCleanupService(intents, media, storage, () -> NOW).cleanup();

		verify(storage).delete(new StorageObjectKey("media/user/trip-record/orphan.jpg"));
		verify(media).markPurged(mediaId, NOW);
		verify(intents).markPurged(intentId);
	}

	@Test
	void continuesWithLaterObjectsWhenOneDeletionFails() {
		MediaUploadIntentMapper intents = mock(MediaUploadIntentMapper.class);
		MediaFileMapper media = mock(MediaFileMapper.class);
		ObjectStorageGateway storage = mock(ObjectStorageGateway.class);
		OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
		UUID intentId = UUID.randomUUID();
		UUID mediaId = UUID.randomUUID();
		when(intents.findExpiredPending(now, 100)).thenReturn(List.of(new MediaUploadIntentRow(
			intentId, UUID.randomUUID(), "media/user/trip-record/failing.jpg", "PENDING", null,
			now.minusMinutes(1), now.minusDays(1), null
		)));
		when(intents.claimPendingForPurge(intentId)).thenReturn(1);
		when(intents.findExpiredCompletedUnlinked(now, 100)).thenReturn(List.of());
		when(media.findDueForPurge(NOW, 100)).thenReturn(List.of(new MediaFileRow(
			mediaId, UUID.randomUUID(), "S3_COMPATIBLE", "bucket", "media/user/trip-record/later.jpg",
			null, "image/jpeg", 10L, 1, 1, null, null, "DELETED", now.minusDays(8), now.minusDays(7), now
		)));
		doThrow(new IllegalStateException("storage unavailable"))
			.when(storage).delete(new StorageObjectKey("media/user/trip-record/failing.jpg"));

		new MediaStorageCleanupService(intents, media, storage, () -> NOW).cleanup();

		verify(storage).delete(new StorageObjectKey("media/user/trip-record/later.jpg"));
		verify(media).markPurged(mediaId, NOW);
	}
}
