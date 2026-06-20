package com.soomgil.global.storage;

import java.util.Objects;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** S3/MinIO 공통 client와 presigner를 구성한다. */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfig {

	@Bean
	S3Client s3Client(S3StorageProperties properties) {
		return S3Client.builder()
			.endpointOverride(Objects.requireNonNull(properties.endpoint(), "S3 endpoint is required"))
			.region(Region.of(properties.region()))
			.credentialsProvider(credentials(properties))
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.httpClientBuilder(UrlConnectionHttpClient.builder())
			.build();
	}

	@Bean
	S3Presigner s3Presigner(S3StorageProperties properties) {
		return S3Presigner.builder()
			.endpointOverride(properties.endpoint())
			.region(Region.of(properties.region()))
			.credentialsProvider(credentials(properties))
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.build();
	}

	@Bean
	MediaContentInspector mediaContentInspector() {
		return new MediaContentInspector();
	}

	private StaticCredentialsProvider credentials(S3StorageProperties properties) {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(
			properties.accessKey(), properties.secretKey()
		));
	}
}
