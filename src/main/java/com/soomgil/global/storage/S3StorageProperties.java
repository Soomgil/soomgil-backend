package com.soomgil.global.storage;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** S3 호환 storage의 서버 endpoint, 브라우저 서명 endpoint, credential, bucket과 공개 URL 설정. */
@ConfigurationProperties("soomgil.storage.s3")
public record S3StorageProperties(
	URI endpoint,
	URI presignEndpoint,
	String region,
	String bucket,
	String accessKey,
	String secretKey,
	URI publicBaseUrl
) {
	/** 브라우저가 접근할 서명 endpoint이며 별도 값이 없으면 서버 endpoint를 사용한다. */
	public URI effectivePresignEndpoint() {
		return presignEndpoint == null ? endpoint : presignEndpoint;
	}
}
