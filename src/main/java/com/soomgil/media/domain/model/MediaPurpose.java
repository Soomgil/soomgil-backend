package com.soomgil.media.domain.model;

/**
 * 업로드 목적별 허용 형식과 공개 범위를 구분하는 값.
 *
 * <p>{@code TRIP_RECORD}는 여행 멤버에게만 공개될 수 있으므로 public URL을 저장하지 않는다.
 */
public enum MediaPurpose {
	PROFILE_IMAGE("profile-image", true),
	TRIP_RECORD("trip-record", false),
	COMMUNITY_POST("community-post", true);

	private final String keySegment;
	private final boolean publicServingAllowed;

	MediaPurpose(String keySegment, boolean publicServingAllowed) {
		this.keySegment = keySegment;
		this.publicServingAllowed = publicServingAllowed;
	}

	public String keySegment() {
		return keySegment;
	}

	public boolean publicServingAllowed() {
		return publicServingAllowed;
	}

	public static MediaPurpose fromKeySegment(String value) {
		for (MediaPurpose purpose : values()) {
			if (purpose.keySegment.equals(value)) {
				return purpose;
			}
		}
		throw new IllegalArgumentException("Unknown media purpose key segment: " + value);
	}
}
