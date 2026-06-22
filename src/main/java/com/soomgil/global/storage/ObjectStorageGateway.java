package com.soomgil.global.storage;

/**
 * S3 호환 object storage와 media application 사이의 경계.
 *
 * <p>업로드 URL 발급과 업로드 완료 object 검증만 제공하며 application은 SDK 타입에 의존하지 않는다.
 */
public interface ObjectStorageGateway {

	PresignedStorageUpload presignUpload(StorageUploadRequest request);

	/**
	 * 제한 시간 동안 object를 읽을 수 있는 URL을 발급한다.
	 *
	 * <p>이 경계는 권한을 판단하지 않으므로 호출 application이 먼저 resource 접근 권한을 검증해야 한다.
	 */
	PresignedStorageRead presignRead(StorageReadRequest request);

	StoredObject inspect(StorageObjectKey objectKey);

	/** object storage에서 key를 멱등 삭제한다. */
	void delete(StorageObjectKey objectKey);
}
