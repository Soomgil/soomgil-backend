package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class MinioComposeConfigurationTest {

	@Test
	void initializesTheConfiguredBucketAfterMinioBecomesHealthy() throws IOException {
		Map<String, Object> compose = new Yaml().load(Files.readString(Path.of("compose.yaml")));
		Map<String, Object> services = map(compose.get("services"));
		Map<String, Object> minio = map(services.get("minio"));
		Map<String, Object> initializer = map(services.get("minio-init"));
		Map<String, Object> dependsOn = map(initializer.get("depends_on"));
		Map<String, Object> minioDependency = map(dependsOn.get("minio"));
		Map<String, Object> environment = map(initializer.get("environment"));

		assertThat(minio).containsKey("healthcheck");
		assertThat(minioDependency).containsEntry("condition", "service_healthy");
		assertThat(environment.get("S3_BUCKET")).isEqualTo("${S3_BUCKET:-soomgil-local}");
		assertThat(initializer.get("entrypoint").toString())
			.contains("mc alias set local http://minio:9000")
			.contains("mc mb --ignore-existing local/$$S3_BUCKET");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> map(Object value) {
		return (Map<String, Object>) value;
	}
}
