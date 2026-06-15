package com.soomgil.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StorageObjectKeyTest {

	@Test
	void createsStorageObjectKey() {
		StorageObjectKey key = new StorageObjectKey("media/users/user-1/avatar.png");

		assertThat(key.value()).isEqualTo("media/users/user-1/avatar.png");
	}

	@Test
	void rejectsUnsafeStorageObjectKeys() {
		assertThatThrownBy(() -> new StorageObjectKey(" ")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new StorageObjectKey("/media/avatar.png")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new StorageObjectKey("media/../secret.png")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new StorageObjectKey("media\\avatar.png")).isInstanceOf(IllegalArgumentException.class);
	}
}
