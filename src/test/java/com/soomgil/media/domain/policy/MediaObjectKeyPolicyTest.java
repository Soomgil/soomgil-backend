package com.soomgil.media.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.domain.model.MediaPurpose;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MediaObjectKeyPolicyTest {

	private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID OBJECT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private final MediaObjectKeyPolicy policy = new MediaObjectKeyPolicy();

	@Test
	void createsOwnedKeyWithoutUsingUntrustedFileName() {
		StorageObjectKey key = policy.create(USER_ID, MediaPurpose.PROFILE_IMAGE, OBJECT_ID, "image/jpeg");

		assertThat(key.value()).isEqualTo(
			"media/10000000-0000-0000-0000-000000000001/profile-image/20000000-0000-0000-0000-000000000002.jpg"
		);
		assertThat(policy.requireOwnedPurpose(USER_ID, key)).isEqualTo(MediaPurpose.PROFILE_IMAGE);
	}

	@Test
	void rejectsAnotherUsersKey() {
		StorageObjectKey key = policy.create(UUID.randomUUID(), MediaPurpose.TRIP_RECORD, OBJECT_ID, "image/png");

		assertThatThrownBy(() -> policy.requireOwnedPurpose(USER_ID, key))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_OWNER_REQUIRED));
	}
}
