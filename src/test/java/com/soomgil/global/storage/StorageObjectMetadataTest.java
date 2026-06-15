package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class StorageObjectMetadataTest {

	@Test
	void createsStorageObjectMetadata() {
		StorageObjectMetadata metadata = new StorageObjectMetadata(
			"soomgil-media",
			new StorageObjectKey("media/users/user-1/avatar.png"),
			"image/png",
			1024L,
			"sha256:abc123",
			URI.create("https://cdn.example.com/media/users/user-1/avatar.png")
		);

		assertThat(metadata.bucket()).isEqualTo("soomgil-media");
		assertThat(metadata.objectKey().value()).isEqualTo("media/users/user-1/avatar.png");
		assertThat(metadata.contentType()).isEqualTo("image/png");
		assertThat(metadata.sizeBytes()).isEqualTo(1024L);
		assertThat(metadata.checksumSha256()).isEqualTo("sha256:abc123");
		assertThat(metadata.publicUrl()).hasToString("https://cdn.example.com/media/users/user-1/avatar.png");
	}

	@Test
	void allowsMissingPublicUrl() {
		StorageObjectMetadata metadata = new StorageObjectMetadata(
			"soomgil-private",
			new StorageObjectKey("award-photos/2026/photo.jpg"),
			"image/jpeg",
			2048L,
			null,
			null
		);

		assertThat(metadata.publicUrl()).isNull();
	}

	@Test
	void rejectsInvalidMetadata() {
		assertThatThrownBy(() -> new StorageObjectMetadata(
			"",
			new StorageObjectKey("media/avatar.png"),
			"image/png",
			1L,
			null,
			null
		)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> new StorageObjectMetadata(
			"soomgil-media",
			new StorageObjectKey("media/avatar.png"),
			"image/png",
			-1L,
			null,
			null
		)).isInstanceOf(IllegalArgumentException.class);
	}
}
