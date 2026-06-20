package com.soomgil.media.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.global.storage.ObjectStorageGateway;
import com.soomgil.global.storage.PresignedStorageUpload;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.global.storage.StorageUploadRequest;
import com.soomgil.media.application.command.dto.CreateUploadUrlCommand;
import com.soomgil.media.application.command.dto.UploadUrlView;
import com.soomgil.media.domain.policy.MediaObjectKeyPolicy;
import com.soomgil.media.domain.policy.MediaUploadPolicy;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/** 검증된 목적과 크기로 10분간 유효한 사용자 소유 upload URL을 발급한다. */
@Service
public class CreateUploadUrlCommandHandler implements CommandHandler<CreateUploadUrlCommand, UploadUrlView> {

	private static final Duration UPLOAD_VALIDITY = Duration.ofMinutes(10);
	private final ObjectStorageGateway storage;
	private final MediaUploadPolicy uploadPolicy;
	private final MediaObjectKeyPolicy keyPolicy;
	private final Supplier<UUID> idGenerator;

	@Autowired
	public CreateUploadUrlCommandHandler(
		ObjectStorageGateway storage,
		MediaUploadPolicy uploadPolicy,
		MediaObjectKeyPolicy keyPolicy
	) {
		this(storage, uploadPolicy, keyPolicy, Ids::newUuid);
	}

	CreateUploadUrlCommandHandler(
		ObjectStorageGateway storage,
		MediaUploadPolicy uploadPolicy,
		MediaObjectKeyPolicy keyPolicy,
		Supplier<UUID> idGenerator
	) {
		this.storage = storage;
		this.uploadPolicy = uploadPolicy;
		this.keyPolicy = keyPolicy;
		this.idGenerator = idGenerator;
	}

	@Override
	public UploadUrlView handle(CreateUploadUrlCommand command) {
		uploadPolicy.validate(command.purpose(), command.mimeType(), command.byteSize());
		StorageObjectKey key = keyPolicy.create(
			command.userId(), command.purpose(), idGenerator.get(), command.mimeType()
		);
		PresignedStorageUpload upload = storage.presignUpload(new StorageUploadRequest(
			key, command.mimeType(), command.byteSize(), UPLOAD_VALIDITY
		));
		return new UploadUrlView(
			upload.uploadUrl(), upload.method(), key.value(), upload.headers(), upload.expiresAt()
		);
	}
}
