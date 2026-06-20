package com.soomgil.media.domain.policy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.media.domain.model.MediaPurpose;
import java.util.UUID;

/**
 * 사용자 소유 media object key를 생성하고 소유권을 검증한다.
 *
 * <p>사용자 입력 파일명은 key에 넣지 않아 path traversal과 제어 문자 문제를 차단한다.
 */
public final class MediaObjectKeyPolicy {

	public StorageObjectKey create(UUID userId, MediaPurpose purpose, UUID objectId, String mimeType) {
		return new StorageObjectKey("media/%s/%s/%s.%s".formatted(
			userId,
			purpose.keySegment(),
			objectId,
			extension(mimeType)
		));
	}

	public MediaPurpose requireOwnedPurpose(UUID userId, StorageObjectKey objectKey) {
		String[] segments = objectKey.value().split("/");
		if (segments.length != 4 || !"media".equals(segments[0]) || !userId.toString().equals(segments[1])) {
			throw new BusinessException(ErrorCode.MEDIA_OWNER_REQUIRED);
		}
		try {
			return MediaPurpose.fromKeySegment(segments[2]);
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.MEDIA_OWNER_REQUIRED);
		}
	}

	private String extension(String mimeType) {
		return switch (mimeType) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "video/mp4" -> "mp4";
			default -> throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
		};
	}
}
