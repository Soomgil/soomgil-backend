package com.soomgil.media.domain.policy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.domain.model.MediaPurpose;
import java.util.Map;
import java.util.Set;

/**
 * 업로드 목적별 MIME allowlist와 최대 byte 크기를 검증한다.
 *
 * <p>값은 업로드 URL 발급과 업로드 완료 검증에서 동일하게 적용해야 한다.
 */
public final class MediaUploadPolicy {

	private static final long MIB = 1024L * 1024L;
	private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
	private static final Map<MediaPurpose, Long> SIZE_LIMITS = Map.of(
		MediaPurpose.PROFILE_IMAGE, 5 * MIB,
		MediaPurpose.TRIP_RECORD, 100 * MIB,
		MediaPurpose.COMMUNITY_POST, 10 * MIB
	);

	public void validate(MediaPurpose purpose, String mimeType, long byteSize) {
		if (!allowedMimeTypes(purpose).contains(mimeType)) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
		}
		if (byteSize <= 0 || byteSize > SIZE_LIMITS.get(purpose)) {
			throw new BusinessException(ErrorCode.MEDIA_SIZE_LIMIT_EXCEEDED);
		}
	}

	private Set<String> allowedMimeTypes(MediaPurpose purpose) {
		if (purpose == MediaPurpose.TRIP_RECORD) {
			return Set.of("image/jpeg", "image/png", "video/mp4");
		}
		return IMAGE_TYPES;
	}
}
