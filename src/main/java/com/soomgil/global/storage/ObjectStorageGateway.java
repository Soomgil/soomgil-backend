package com.soomgil.global.storage;

/**
 * S3 호환 object storage와 media application 사이의 경계.
 *
 * <p>업로드 URL 발급과 업로드 완료 object 검증만 제공하며 application은 SDK 타입에 의존하지 않는다.
 */
public interface ObjectStorageGateway {

	PresignedStorageUpload presignUpload(StorageUploadRequest request);

	StoredObject inspect(StorageObjectKey objectKey);

	default byte[] read(StorageObjectKey objectKey) {
		throw new UnsupportedOperationException("Object read is not implemented.");
	}
}
