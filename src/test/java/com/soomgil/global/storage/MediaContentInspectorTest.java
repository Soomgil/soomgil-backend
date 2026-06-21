package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

class MediaContentInspectorTest {

	private final MediaContentInspector inspector = new MediaContentInspector();

	@Test
	void detectsSupportedFileSignatures() {
		assertThat(inspector.detect(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}))
			.isEqualTo("image/jpeg");
		assertThat(inspector.detect(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}))
			.isEqualTo("image/png");
		assertThat(inspector.detect("0000ftypisom".getBytes(StandardCharsets.US_ASCII)))
			.isEqualTo("video/mp4");
	}

	@Test
	void returnsNullForUnknownContent() {
		assertThat(inspector.detect("plain text".getBytes(StandardCharsets.UTF_8))).isNull();
	}

	@Test
	void supportsClassBasedObservabilityProxy() {
		ProxyFactory proxyFactory = new ProxyFactory(inspector);
		proxyFactory.setProxyTargetClass(true);

		MediaContentInspector proxy = (MediaContentInspector) proxyFactory.getProxy();

		assertThat(proxy.detect(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}))
			.isEqualTo("image/jpeg");
	}
}
