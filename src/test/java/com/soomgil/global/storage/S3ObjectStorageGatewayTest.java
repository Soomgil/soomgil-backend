package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ObjectStorageGatewayTest {

	private final S3StorageProperties properties = new S3StorageProperties(
		URI.create("http://localhost:9000"), "ap-northeast-2", "soomgil-media",
		"access-key", "secret-key", URI.create("https://cdn.example.com")
	);

	@Test
	void createsAwsV4SignedPutUrl() {
		S3StorageConfig config = new S3StorageConfig();
		try (S3Presigner presigner = config.s3Presigner(properties)) {
			S3ObjectStorageGateway gateway = new S3ObjectStorageGateway(
				mock(S3Client.class), presigner, properties, new MediaContentInspector()
			);

			PresignedStorageUpload upload = gateway.presignUpload(new StorageUploadRequest(
				new StorageObjectKey("media/user/profile-image/file.jpg"),
				"image/jpeg", 1024L, Duration.ofMinutes(10)
			));

			assertThat(upload.method()).isEqualTo("PUT");
			assertThat(upload.uploadUrl().getQuery()).contains("X-Amz-Signature=");
			assertThat(upload.uploadUrl().getPath()).endsWith("/soomgil-media/media/user/profile-image/file.jpg");
		}
	}

	@Test
	void createsThirtyMinuteAwsV4SignedReadUrl() {
		S3StorageConfig config = new S3StorageConfig();
		try (S3Presigner presigner = config.s3Presigner(properties)) {
			S3ObjectStorageGateway gateway = new S3ObjectStorageGateway(
				mock(S3Client.class), presigner, properties, new MediaContentInspector()
			);

			PresignedStorageRead read = gateway.presignRead(new StorageReadRequest(
				new StorageObjectKey("media/user/trip-record/file.jpg"),
				Duration.ofMinutes(30)
			));

			assertThat(read.readUrl().getQuery()).contains("X-Amz-Signature=").contains("X-Amz-Expires=1800");
			assertThat(read.readUrl().getPath()).endsWith("/soomgil-media/media/user/trip-record/file.jpg");
		}
	}

	@Test
	void inspectsStoredImageBytesAndDimensions() throws Exception {
		byte[] png = png(3, 2);
		S3Client client = mock(S3Client.class);
		when(client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
			.contentType("image/png").contentLength((long) png.length).checksumSHA256("checksum").build());
		when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
			ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), png)
		);
		S3StorageConfig config = new S3StorageConfig();
		try (S3Presigner presigner = config.s3Presigner(properties)) {
			S3ObjectStorageGateway gateway = new S3ObjectStorageGateway(
				client, presigner, properties, new MediaContentInspector()
			);

			StoredObject object = gateway.inspect(new StorageObjectKey("media/user/trip-record/file.png"));

			assertThat(object.detectedContentType()).isEqualTo("image/png");
			assertThat(object.width()).isEqualTo(3);
			assertThat(object.height()).isEqualTo(2);
			assertThat(object.metadata().publicUrl())
				.hasToString("https://cdn.example.com/media/user/trip-record/file.png");
		}
	}

	private byte[] png(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return output.toByteArray();
	}
}
