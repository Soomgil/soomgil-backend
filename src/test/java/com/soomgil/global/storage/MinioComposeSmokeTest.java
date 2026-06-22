package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Testcontainers(disabledWithoutDocker = true)
class MinioComposeSmokeTest {

	@Test
	void productionComposeCreatesBucketOnAnEmptyVolume() throws Exception {
		List<String> compose = composeCommand();
		String project = "soomgil-minio-smoke-" + UUID.randomUUID().toString().substring(0, 8);
		Map<String, String> environment = Map.of(
			"S3_ACCESS_KEY", "smoke-access",
			"S3_SECRET_KEY", "smoke-secret-key",
			"S3_BUCKET", "smoke-bucket",
			"S3_PORT", "0",
			"S3_CONSOLE_PORT", "0"
		);
		try {
			run(compose, project, environment, List.of("up", "-d", "minio", "minio-init"));
			run(compose, project, environment, List.of("wait", "minio-init"));
			String portOutput = run(compose, project, environment, List.of("port", "minio", "9000")).trim();
			int port = Integer.parseInt(portOutput.substring(portOutput.lastIndexOf(':') + 1));
			S3StorageProperties properties = new S3StorageProperties(
				URI.create("http://localhost:" + port), "ap-northeast-2", "smoke-bucket",
				"smoke-access", "smoke-secret-key", null
			);
			try (S3Client client = new S3StorageConfig().s3Client(properties)) {
				assertThat(client.headBucket(HeadBucketRequest.builder().bucket("smoke-bucket").build())
					.sdkHttpResponse().isSuccessful()).isTrue();
			}
		}
		finally {
			run(compose, project, environment, List.of("down", "-v", "--remove-orphans"));
		}
	}

	private List<String> composeCommand() throws Exception {
		if (commandSucceeds(List.of("docker", "compose", "version"))) return List.of("docker", "compose");
		if (commandSucceeds(List.of("docker-compose", "version"))) return List.of("docker-compose");
		throw new IllegalStateException("Docker Compose command is unavailable.");
	}

	private boolean commandSucceeds(List<String> command) throws Exception {
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		return process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0;
	}

	private String run(
		List<String> compose, String project, Map<String, String> environment, List<String> arguments
	) throws Exception {
		List<String> command = new ArrayList<>(compose);
		command.addAll(List.of("-f", Path.of("compose.yaml").toAbsolutePath().toString(), "-p", project));
		command.addAll(arguments);
		ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
		builder.environment().putAll(environment);
		Process process = builder.start();
		boolean finished = process.waitFor(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS);
		if (!finished) {
			process.destroyForcibly();
			process.waitFor(10, TimeUnit.SECONDS);
		}
		String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		if (!finished || process.exitValue() != 0) {
			throw new IllegalStateException("Compose command failed: " + String.join(" ", command) + "\n" + output);
		}
		return output;
	}
}
