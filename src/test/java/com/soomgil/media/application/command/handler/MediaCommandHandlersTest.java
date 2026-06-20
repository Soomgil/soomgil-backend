package com.soomgil.media.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.ObjectStorageGateway;
import com.soomgil.global.storage.PresignedStorageUpload;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.global.storage.StorageObjectMetadata;
import com.soomgil.global.storage.StorageUploadRequest;
import com.soomgil.global.storage.StoredObject;
import com.soomgil.media.application.command.dto.CreateMediaFileCommand;
import com.soomgil.media.application.command.dto.CreateUploadUrlCommand;
import com.soomgil.media.application.command.dto.DeleteMediaFileCommand;
import com.soomgil.media.application.port.LinkedMediaResourceAuthorizer;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.domain.model.MediaFileMetadata;
import com.soomgil.media.domain.model.MediaPurpose;
import com.soomgil.media.domain.policy.MediaObjectKeyPolicy;
import com.soomgil.media.domain.policy.MediaUploadPolicy;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class MediaCommandHandlersTest {

	private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private static final UUID GENERATED_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-06-20T12:00:00Z");
	private static final TimeProvider TIME = () -> NOW;
	private static final Supplier<UUID> IDS = () -> GENERATED_ID;

	private final MediaUploadPolicy uploadPolicy = new MediaUploadPolicy();
	private final MediaObjectKeyPolicy keyPolicy = new MediaObjectKeyPolicy();

	@Test
	void createsPresignedUploadForOwnedPurposeKey() {
		FakeStorage storage = new FakeStorage();
		CreateUploadUrlCommandHandler handler = new CreateUploadUrlCommandHandler(
			storage, uploadPolicy, keyPolicy, IDS
		);

		var result = handler.handle(new CreateUploadUrlCommand(
			USER_ID, "../../avatar.jpg", "image/jpeg", 1024L, MediaPurpose.PROFILE_IMAGE
		));

		assertThat(result.objectKey()).isEqualTo(
			"media/10000000-0000-0000-0000-000000000001/profile-image/20000000-0000-0000-0000-000000000001.jpg"
		);
		assertThat(result.method()).isEqualTo("PUT");
		assertThat(storage.uploadRequest.contentType()).isEqualTo("image/jpeg");
		assertThat(storage.uploadRequest.byteSize()).isEqualTo(1024L);
	}

	@Test
	void registersVerifiedObjectAndSuppressesPrivatePublicUrl() {
		FakeStorage storage = new FakeStorage();
		StorageObjectKey key = keyPolicy.create(USER_ID, MediaPurpose.TRIP_RECORD, GENERATED_ID, "image/png");
		storage.storedObject = storedObject(key, "image/png", "image/png", 2048L);
		FakeRepository repository = new FakeRepository();
		CreateMediaFileCommandHandler handler = new CreateMediaFileCommandHandler(
			storage, repository, (userId, type, resourceId) -> true,
			uploadPolicy, keyPolicy, TIME, IDS
		);

		MediaFileMetadata result = handler.handle(new CreateMediaFileCommand(
			USER_ID, key.value(), URI.create("https://untrusted.example/file.png"), "image/png", 2048L,
			800, 600, null, null
		));

		assertThat(result.publicUrl()).isNull();
		assertThat(result.status()).isEqualTo("ACTIVE");
		assertThat(repository.saved).isEqualTo(result);
	}

	@Test
	void rejectsMetadataThatDoesNotMatchStoredObject() {
		FakeStorage storage = new FakeStorage();
		StorageObjectKey key = keyPolicy.create(USER_ID, MediaPurpose.COMMUNITY_POST, GENERATED_ID, "image/jpeg");
		storage.storedObject = storedObject(key, "image/jpeg", "image/png", 2048L);
		FakeRepository repository = new FakeRepository();
		CreateMediaFileCommandHandler handler = new CreateMediaFileCommandHandler(
			storage, repository, (userId, type, resourceId) -> true,
			uploadPolicy, keyPolicy, TIME, IDS
		);

		assertThatThrownBy(() -> handler.handle(new CreateMediaFileCommand(
			USER_ID, key.value(), null, "image/jpeg", 2048L, null, null, null, null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_METADATA_MISMATCH));
		assertThat(repository.saved).isNull();
	}

	@Test
	void rejectsForbiddenResourceLink() {
		FakeStorage storage = new FakeStorage();
		StorageObjectKey key = keyPolicy.create(USER_ID, MediaPurpose.COMMUNITY_POST, GENERATED_ID, "image/jpeg");
		storage.storedObject = storedObject(key, "image/jpeg", "image/jpeg", 2048L);
		CreateMediaFileCommandHandler handler = new CreateMediaFileCommandHandler(
			storage, new FakeRepository(), (userId, type, resourceId) -> false,
			uploadPolicy, keyPolicy, TIME, IDS
		);

		assertThatThrownBy(() -> handler.handle(new CreateMediaFileCommand(
			USER_ID, key.value(), null, "image/jpeg", 2048L, null, null,
			"COMMUNITY_POST", UUID.randomUUID()
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_LINK_FORBIDDEN));
	}

	@Test
	void softDeletesOwnedMediaAndSchedulesPurgeAfterSevenDays() {
		FakeRepository repository = new FakeRepository();
		repository.found = metadata(USER_ID, "ACTIVE");
		DeleteMediaFileCommandHandler handler = new DeleteMediaFileCommandHandler(repository, TIME);

		handler.handle(new DeleteMediaFileCommand(USER_ID, GENERATED_ID));

		assertThat(repository.deletedAt).isEqualTo(NOW);
		assertThat(repository.purgeAfter).isEqualTo(NOW.plusSeconds(7 * 24 * 60 * 60L));
	}

	@Test
	void rejectsDeleteByNonOwner() {
		FakeRepository repository = new FakeRepository();
		repository.found = metadata(OTHER_USER_ID, "ACTIVE");
		DeleteMediaFileCommandHandler handler = new DeleteMediaFileCommandHandler(repository, TIME);

		assertThatThrownBy(() -> handler.handle(new DeleteMediaFileCommand(USER_ID, GENERATED_ID)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_OWNER_REQUIRED));
	}

	private StoredObject storedObject(
		StorageObjectKey key,
		String declaredType,
		String detectedType,
		long size
	) {
		return new StoredObject(
			new StorageObjectMetadata(
				"soomgil-media", key, declaredType, size, "sha256:test",
				URI.create("https://cdn.example.com/" + key.value())
			),
			detectedType,
			800,
			600
		);
	}

	private MediaFileMetadata metadata(UUID ownerId, String status) {
		return new MediaFileMetadata(
			GENERATED_ID, ownerId, "S3_COMPATIBLE", "soomgil-media",
			new StorageObjectKey("media/" + ownerId + "/profile-image/file.jpg"),
			URI.create("https://cdn.example.com/file.jpg"), "image/jpeg", 1024L,
			100, 100, null, null, status,
			OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), null, null
		);
	}

	private static final class FakeStorage implements ObjectStorageGateway {
		private StorageUploadRequest uploadRequest;
		private StoredObject storedObject;

		@Override
		public PresignedStorageUpload presignUpload(StorageUploadRequest request) {
			this.uploadRequest = request;
			return new PresignedStorageUpload(
				URI.create("https://storage.example.com/upload"), "PUT",
				Map.of("Content-Type", request.contentType()),
				OffsetDateTime.ofInstant(NOW.plusSeconds(600), ZoneOffset.UTC)
			);
		}

		@Override
		public StoredObject inspect(StorageObjectKey objectKey) {
			return storedObject;
		}
	}

	private static final class FakeRepository implements MediaFileRepository {
		private MediaFileMetadata saved;
		private MediaFileMetadata found;
		private Instant deletedAt;
		private Instant purgeAfter;

		@Override
		public void save(MediaFileMetadata mediaFile) {
			this.saved = mediaFile;
		}

		@Override
		public MediaFileMetadata findById(UUID mediaFileId) {
			return found;
		}

		@Override
		public boolean markDeleted(UUID mediaFileId, Instant deletedAt, Instant purgeAfter) {
			this.deletedAt = deletedAt;
			this.purgeAfter = purgeAfter;
			return true;
		}
	}
}
