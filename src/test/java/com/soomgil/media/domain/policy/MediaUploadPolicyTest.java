package com.soomgil.media.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.domain.model.MediaPurpose;
import org.junit.jupiter.api.Test;

class MediaUploadPolicyTest {

	private final MediaUploadPolicy policy = new MediaUploadPolicy();

	@Test
	void acceptsMimeTypeAndSizeAllowedForPurpose() {
		policy.validate(MediaPurpose.PROFILE_IMAGE, "image/jpeg", 5 * 1024 * 1024L);
		policy.validate(MediaPurpose.TRIP_RECORD, "video/mp4", 100 * 1024 * 1024L);
		policy.validate(MediaPurpose.COMMUNITY_POST, "image/png", 10 * 1024 * 1024L);
	}

	@Test
	void rejectsUnsupportedMimeType() {
		assertThatThrownBy(() -> policy.validate(MediaPurpose.PROFILE_IMAGE, "video/mp4", 1024L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
	}

	@Test
	void rejectsNonPositiveOrOversizedFile() {
		assertThatThrownBy(() -> policy.validate(MediaPurpose.COMMUNITY_POST, "image/jpeg", 0L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_SIZE_LIMIT_EXCEEDED));

		assertThatThrownBy(() -> policy.validate(
			MediaPurpose.PROFILE_IMAGE,
			"image/jpeg",
			5 * 1024 * 1024L + 1
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEDIA_SIZE_LIMIT_EXCEEDED));
	}
}
