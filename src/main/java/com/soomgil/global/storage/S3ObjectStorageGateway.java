package com.soomgil.global.storage;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

/**
 * AWS SDK v2로 S3 호환 storage의 PUT URL을 발급하고 업로드 object를 검사한다.
 *
 * <p>검사 시 HEAD metadata와 magic bytes를 함께 읽으며 이미지는 서버가 실제 크기도 판별한다.
 */
@Component
public class S3ObjectStorageGateway implements ObjectStorageGateway {

	private final S3Client client;
	private final S3Presigner presigner;
	private final S3StorageProperties properties;
	private final MediaContentInspector contentInspector;

	public S3ObjectStorageGateway(
		S3Client client,
		S3Presigner presigner,
		S3StorageProperties properties,
		MediaContentInspector contentInspector
	) {
		this.client = client;
		this.presigner = presigner;
		this.properties = properties;
		this.contentInspector = contentInspector;
	}

	@Override
	public PresignedStorageUpload presignUpload(StorageUploadRequest request) {
		PutObjectRequest put = PutObjectRequest.builder()
			.bucket(properties.bucket())
			.key(request.objectKey().value())
			.contentType(request.contentType())
			.contentLength(request.byteSize())
			.build();
		PresignedPutObjectRequest signed = presigner.presignPutObject(PutObjectPresignRequest.builder()
			.signatureDuration(request.validity())
			.putObjectRequest(put)
			.build());
		Map<String, String> headers = new LinkedHashMap<>();
		signed.signedHeaders().forEach((name, values) -> headers.put(name, String.join(",", values)));
		return new PresignedStorageUpload(
			URI.create(signed.url().toString()), signed.httpRequest().method().name(), headers,
			OffsetDateTime.now(ZoneOffset.UTC).plus(request.validity())
		);
	}

	@Override
	public PresignedStorageRead presignRead(StorageReadRequest request) {
		GetObjectRequest get = GetObjectRequest.builder()
			.bucket(properties.bucket())
			.key(request.objectKey().value())
			.build();
		PresignedGetObjectRequest signed = presigner.presignGetObject(GetObjectPresignRequest.builder()
			.signatureDuration(request.validity())
			.getObjectRequest(get)
			.build());
		return new PresignedStorageRead(
			URI.create(signed.url().toString()),
			OffsetDateTime.now(ZoneOffset.UTC).plus(request.validity())
		);
	}

	@Override
	public StoredObject inspect(StorageObjectKey objectKey) {
		try {
			HeadObjectResponse head = client.headObject(HeadObjectRequest.builder()
				.bucket(properties.bucket()).key(objectKey.value()).build());
			byte[] prefix = client.getObjectAsBytes(GetObjectRequest.builder()
				.bucket(properties.bucket()).key(objectKey.value()).range("bytes=0-31").build()).asByteArray();
			String detectedType = contentInspector.detect(prefix);
			Integer width = null;
			Integer height = null;
			if (detectedType != null && detectedType.startsWith("image/")) {
				ResponseBytes<GetObjectResponse> bytes = client.getObjectAsBytes(GetObjectRequest.builder()
					.bucket(properties.bucket()).key(objectKey.value()).build());
				BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes.asByteArray()));
				if (image != null) {
					width = image.getWidth();
					height = image.getHeight();
				}
			}
			return new StoredObject(new StorageObjectMetadata(
				properties.bucket(), objectKey, head.contentType(), head.contentLength(), head.checksumSHA256(),
				publicUrl(objectKey)
			), detectedType, width, height);
		}
		catch (NoSuchKeyException exception) {
			throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND);
		}
		catch (S3Exception exception) {
			if (exception.statusCode() == 404) {
				throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND);
			}
			throw new IllegalStateException("Object storage inspection failed.", exception);
		}
		catch (SdkException exception) {
			throw new IllegalStateException("Object storage inspection failed.", exception);
		}
		catch (Exception exception) {
			throw new IllegalStateException("Stored media content could not be inspected.", exception);
		}
	}

	@Override
	public void delete(StorageObjectKey objectKey) {
		try {
			client.deleteObject(DeleteObjectRequest.builder()
				.bucket(properties.bucket())
				.key(objectKey.value())
				.build());
		}
		catch (SdkException exception) {
			throw new IllegalStateException("Object storage deletion failed.", exception);
		}
	}

	private URI publicUrl(StorageObjectKey objectKey) {
		if (properties.publicBaseUrl() == null) {
			return null;
		}
		String base = properties.publicBaseUrl().toString();
		return URI.create((base.endsWith("/") ? base : base + "/") + objectKey.value());
	}
}
