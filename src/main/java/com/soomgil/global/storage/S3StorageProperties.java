package com.soomgil.global.storage;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** S3 호환 storage endpoint, credential, bucket과 공개 URL 설정. */
@ConfigurationProperties("soomgil.storage.s3")
public record S3StorageProperties(
	URI endpoint,
	String region,
	String bucket,
	String accessKey,
	String secretKey,
	URI publicBaseUrl
) {
}
