package com.soomgil.media.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.ObjectStorageGateway;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.global.storage.StoredObject;
import com.soomgil.media.application.command.dto.CreateMediaFileCommand;
import com.soomgil.media.application.port.LinkedMediaResourceAuthorizer;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.domain.model.MediaFileMetadata;
import com.soomgil.media.domain.model.MediaPurpose;
import com.soomgil.media.domain.policy.MediaObjectKeyPolicy;
import com.soomgil.media.domain.policy.MediaUploadPolicy;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

/** storage object의 실재·MIME·크기·연결 권한을 확인한 뒤 metadata를 등록한다. */
@Service
public class CreateMediaFileCommandHandler implements CommandHandler<CreateMediaFileCommand, MediaFileMetadata> {

	private final ObjectStorageGateway storage;
	private final MediaFileRepository repository;
	private final LinkedMediaResourceAuthorizer resourceAuthorizer;
	private final MediaUploadPolicy uploadPolicy;
	private final MediaObjectKeyPolicy keyPolicy;
	private final TimeProvider timeProvider;
	private final Supplier<UUID> idGenerator;

	@Autowired
	public CreateMediaFileCommandHandler(
		ObjectStorageGateway storage,
		MediaFileRepository repository,
		LinkedMediaResourceAuthorizer resourceAuthorizer,
		MediaUploadPolicy uploadPolicy,
		MediaObjectKeyPolicy keyPolicy,
		TimeProvider timeProvider
	) {
		this(storage, repository, resourceAuthorizer, uploadPolicy, keyPolicy, timeProvider, Ids::newUuid);
	}

	CreateMediaFileCommandHandler(
		ObjectStorageGateway storage,
		MediaFileRepository repository,
		LinkedMediaResourceAuthorizer resourceAuthorizer,
		MediaUploadPolicy uploadPolicy,
		MediaObjectKeyPolicy keyPolicy,
		TimeProvider timeProvider,
		Supplier<UUID> idGenerator
	) {
		this.storage = storage;
		this.repository = repository;
		this.resourceAuthorizer = resourceAuthorizer;
		this.uploadPolicy = uploadPolicy;
		this.keyPolicy = keyPolicy;
		this.timeProvider = timeProvider;
		this.idGenerator = idGenerator;
	}

	@Transactional
	@Override
	public MediaFileMetadata handle(CreateMediaFileCommand command) {
		StorageObjectKey key = new StorageObjectKey(command.objectKey());
		MediaPurpose purpose = keyPolicy.requireOwnedPurpose(command.userId(), key);
		uploadPolicy.validate(purpose, command.mimeType(), command.byteSize());
		validateLink(command, purpose);

		StoredObject object = storage.inspect(key);
		if (!metadataMatches(command, object)) {
			throw new BusinessException(ErrorCode.MEDIA_METADATA_MISMATCH);
		}

		OffsetDateTime createdAt = OffsetDateTime.ofInstant(timeProvider.now(), ZoneOffset.UTC);
		UUID mediaFileId = idGenerator.get();
		MediaFileMetadata mediaFile = new MediaFileMetadata(
			mediaFileId, command.userId(), "S3_COMPATIBLE", object.metadata().bucket(), key,
			purpose.publicServingAllowed()
				? URI.create("/api/v1/media/files/" + mediaFileId + "/content") : null,
			object.detectedContentType(), object.metadata().sizeBytes(), object.width(), object.height(),
			command.linkedResourceType(), command.linkedResourceId(), "ACTIVE", createdAt, null, null
		);
		repository.save(mediaFile);
		return mediaFile;
	}

	private void validateLink(CreateMediaFileCommand command, MediaPurpose purpose) {
		boolean hasType = command.linkedResourceType() != null && !command.linkedResourceType().isBlank();
		boolean hasId = command.linkedResourceId() != null;
		if (hasType != hasId) {
			throw new BusinessException(ErrorCode.MEDIA_LINK_FORBIDDEN);
		}
		if (hasType && !purposeMatchesResource(purpose, command.linkedResourceType())) {
			throw new BusinessException(ErrorCode.MEDIA_LINK_FORBIDDEN);
		}
		if (hasType && !resourceAuthorizer.canLink(
			command.userId(), command.linkedResourceType(), command.linkedResourceId()
		)) {
			throw new BusinessException(ErrorCode.MEDIA_LINK_FORBIDDEN);
		}
	}

	private boolean purposeMatchesResource(MediaPurpose purpose, String resourceType) {
		return switch (purpose) {
			case PROFILE_IMAGE -> "USER_PROFILE".equals(resourceType);
			case TRIP_RECORD -> "TRIP_RECORD".equals(resourceType);
			case COMMUNITY_POST -> "COMMUNITY_POST".equals(resourceType);
		};
	}

	private boolean metadataMatches(CreateMediaFileCommand command, StoredObject object) {
		return command.byteSize() == object.metadata().sizeBytes()
			&& Objects.equals(command.mimeType(), object.metadata().contentType())
			&& Objects.equals(command.mimeType(), object.detectedContentType())
			&& (command.width() == null || Objects.equals(command.width(), object.width()))
			&& (command.height() == null || Objects.equals(command.height(), object.height()));
	}
}
