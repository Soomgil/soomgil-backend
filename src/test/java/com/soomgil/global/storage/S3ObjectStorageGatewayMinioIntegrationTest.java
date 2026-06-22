package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers(disabledWithoutDocker = true)
class S3ObjectStorageGatewayMinioIntegrationTest {

	private static final String ACCESS_KEY = "soomgil-test";
	private static final String SECRET_KEY = "soomgil-test-secret";
	private static final String BUCKET = "soomgil-test";

	@Container
	private static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
		.withEnv("MINIO_ROOT_USER", ACCESS_KEY)
		.withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
		.withCommand("server", "/data")
		.withExposedPorts(9000)
		.waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

	private static S3Client client;
	private static S3Presigner presigner;
	private static S3ObjectStorageGateway gateway;

	@BeforeAll
	static void setUpStorage() {
		URI endpoint = URI.create("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
		S3StorageProperties properties = new S3StorageProperties(
			endpoint, "ap-northeast-2", BUCKET, ACCESS_KEY, SECRET_KEY, null
		);
		S3StorageConfig config = new S3StorageConfig();
		client = config.s3Client(properties);
		presigner = config.s3Presigner(properties);
		client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
		gateway = new S3ObjectStorageGateway(client, presigner, properties, new MediaContentInspector());
	}

	@AfterAll
	static void closeClients() {
		if (presigner != null) presigner.close();
		if (client != null) client.close();
	}

	@Test
	void uploadsAndReadsPrivateBytesThroughSignedUrls() throws Exception {
		byte[] expected = "signed-storage-round-trip".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		StorageObjectKey key = new StorageObjectKey("media/user/trip-record/round-trip.txt");
		PresignedStorageUpload upload = gateway.presignUpload(new StorageUploadRequest(
			key, "text/plain", expected.length, Duration.ofMinutes(10)
		));

		HttpURLConnection put = (HttpURLConnection) upload.uploadUrl().toURL().openConnection();
		put.setRequestMethod("PUT");
		put.setDoOutput(true);
		upload.headers().forEach((name, value) -> {
			if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
				put.setRequestProperty(name, value);
			}
		});
		put.setFixedLengthStreamingMode(expected.length);
		try (var output = put.getOutputStream()) {
			output.write(expected);
		}
		assertThat(put.getResponseCode()).isEqualTo(200);

		PresignedStorageRead read = gateway.presignRead(new StorageReadRequest(key, Duration.ofMinutes(30)));
		HttpURLConnection get = (HttpURLConnection) read.readUrl().toURL().openConnection();
		assertThat(get.getResponseCode()).isEqualTo(200);
		try (InputStream input = get.getInputStream()) {
			assertThat(input.readAllBytes()).isEqualTo(expected);
		}

		gateway.delete(key);
		HttpURLConnection deleted = (HttpURLConnection) gateway.presignRead(
			new StorageReadRequest(key, Duration.ofMinutes(1))
		).readUrl().toURL().openConnection();
		assertThat(deleted.getResponseCode()).isEqualTo(404);
	}
}
